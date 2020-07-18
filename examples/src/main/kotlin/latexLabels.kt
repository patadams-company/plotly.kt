import hep.dataforge.meta.invoke
import scientifik.plotly.*

@UnstablePlotlyAPI
fun main() {
    val plot = Plotly.plot {
        scatter {
            x(2, 3, 4, 5)
            y(10, 15, 13, 17)
        }

        text {
            position(2, 10)
            font {
                size = 18
            }
            text = "\$\\alpha\$"
        }

        text {
            position(5, 17)
            font {
                size = 18
            }
            text = "\$\\Omega\$"
        }

        layout {
            title {
                text = "Plot with annotations \$\\alpha~and~\\Omega\$"
            }
        }
    }.makeFile(additionalHeaders = listOf(mathJaxHeader))
}
