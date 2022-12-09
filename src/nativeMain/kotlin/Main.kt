import kotlin.native.concurrent.TransferMode
import kotlin.native.concurrent.Worker

const val AB_INIT_VALUE = -1
class Data {
    var x = 0
    var y = 0
    var a = AB_INIT_VALUE
    var b = AB_INIT_VALUE
}

fun main() {
    val t1 = Worker.start()
    val t2 = Worker.start()
    var weakHappened = false
    var wrongABvalues = false
    for (i in 0..10000) {
        if (wrongABvalues) { break }
        val s = Data()
        val f1 = t1.execute(TransferMode.SAFE, { s }) {
            it.x = 1
            it.a = it.y
        }
        val f2 = t2.execute(TransferMode.SAFE, { s }) {
            it.y = 1
            it.b = it.x
        }
        f1.consume {
            f2.consume {
                if (s.a == AB_INIT_VALUE || s.b == AB_INIT_VALUE) {
                    wrongABvalues = true
                }
                if (s.a == 0 && s.b == 0) {
                    weakHappened = true
                    println(i)
                }
            }
        }
    }
    if (wrongABvalues) {
        println("Either a or b is not assigned by the litmus test!")
    } else if (weakHappened) {
        println("The SB weak result IS observed.")
    } else {
        println("The SB weak result is NOT observed.")
    }
}