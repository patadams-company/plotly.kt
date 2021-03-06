package kscience.plotly.server

import hep.dataforge.meta.*
import hep.dataforge.names.Name
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kscience.plotly.Plot


/**
 * A change collector that combines all emitted configuration changes until read, than drops all collected changes
 * and starts new batch.
 */
public class MetaChangeCollector {
    private val mutex = Mutex()
    private var state = Config()

    public suspend fun collect(name: Name, newItem: MetaItem<*>?) {
        mutex.withLock {
            state[name] = newItem
        }
    }

    public suspend fun read(): Meta {
        return if (!state.isEmpty()) {
            mutex.withLock {
                state.seal().also {
                    state = Config()
                }
            }
        } else {
            Meta.EMPTY
        }
    }
}

private fun Config.collectChanges(scope: CoroutineScope): MetaChangeCollector {
    return MetaChangeCollector().apply {
        onChange(this) { name, _, newItem ->
            scope.launch {
                collect(name, newItem)
            }
        }
    }
}

private fun Config.flowChanges(scope: CoroutineScope, updateInterval: Long): Flow<Meta> {
    val collector = collectChanges(scope)
    return flow {
        while (true) {
            delay(updateInterval)
            val meta = collector.read()
            if (!meta.isEmpty()) {
                emit(meta)
            }
        }
    }
}

public fun Plot.collectUpdates(plotId: String, scope: CoroutineScope, updateInterval: Long): Flow<Update> {
    return config.flowChanges(scope, updateInterval).transform { change ->
        change["layout"].node?.let { emit(Update.Layout(plotId, it)) }
        change.getIndexed("data").forEach { (index, metaItem) ->
            if (metaItem is MetaItem.NodeItem) {
                emit(Update.Trace(plotId, index?.toInt() ?: 0, metaItem.node))
            }
        }
    }
}