import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

const val AB_INIT_VALUE = -1
class Data {
    var x = 0
    var y = 0
    var a = AB_INIT_VALUE
    var b = AB_INIT_VALUE
}

const val ITER_NUMBER = 10000
fun litmusTestII(name : String,
                 weakValueA : Int,
                 weakValueB: Int,
                 t1 : Data.() -> Unit,
                 t2 : Data.() -> Unit
) {
    val w1 = Worker.start()
    val w2 = Worker.start()
    var weakIteration : Int? = null
    var wrongABvalues = false
    for (i in 0..ITER_NUMBER) {
        if (wrongABvalues || weakIteration != null) { break }
        val s = Data()
        // `execute` doesn't depend on its TransferMode argument,
        // so its value is irrelevant
        val tm = TransferMode.SAFE
        val f1 = w1.execute(tm, { t1 to s }, { (f, s) -> f(s) })
        val f2 = w2.execute(tm, { t2 to s }, { (f, s) -> f(s) })
        f1.consume {
            f2.consume {
                if (s.a == AB_INIT_VALUE || s.b == AB_INIT_VALUE) {
                    wrongABvalues = true
                }
                if (s.a == weakValueA && s.b == weakValueB) {
                    weakIteration = i
                }
            }
        }
    }
    if (wrongABvalues) {
        println("Either a or b is not assigned by the $name litmus test!")
    } else if (weakIteration != null) {
        println("The $name weak result IS observed on the $weakIteration iteration.")
    } else {
        println("The $name weak result is NOT observed.")
    }
}

fun main() {
    litmusTestII("SB", 0, 0,
        { x = 1; a = y },
        { y = 1; b = x })
    litmusTestII("LB", 1, 1,
        { a = y; x = 1 },
        { b = x; y = 1 })
    litmusTestII("MP", 1, 0,
        { y = 1; x = 1 },
        { a = x; b = y })
}