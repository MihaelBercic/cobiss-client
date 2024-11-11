/**
 * @author Mihael Berčič on 31. 01. 24.
 */
package logging

import kotlinx.serialization.Serializable
import java.io.PrintWriter
import java.io.StringWriter
import java.net.http.HttpClient
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.LinkedBlockingQueue

/**
 * Created by Mihael Berčič
 * on 26/03/2020 15:35
 * using IntelliJ IDEA
 *
 * Used for any type of logging.
 */
object Logger {

    private var isLoggingEnabled = true
    private var currentDebug: DebugType = DebugType.ALL
    private val timeFormatter = DateTimeFormatter.ofPattern("dd. MM | HH:mm:ss.SSS")

    const val red = "\u001b[31m"
    const val blue = "\u001B[34;1m"
    const val cyan = "\u001b[36m"
    const val green = "\u001b[32m"
    const val black = "\u001b[30m"
    const val yellow = "\u001b[33m"
    const val magenta = "\u001b[35m"
    const val white = "\u001b[37m"
    const val reset = "\u001B[0m"

    /** Prints the given message with the coloring and debug information provided.*/
    private fun log(debugType: DebugType, message: Any, color: String = black) {
        if (isLoggingEnabled) {
            val typeString = LocalDateTime.now().format(timeFormatter).padEnd(11) + " | " + padRight(debugType.name)
            val output = "$color$typeString$reset$message"
            println(output)
        }
    }

    /** Enables or disables software logging.  */
    fun toggleLogging(enable: Boolean) {
        isLoggingEnabled = enable
    }

    fun reportException(e: Throwable) {
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        error(sw.toString())
        error(e.cause ?: "Unknown cause.")
        e.cause?.apply {
            val sww = StringWriter()
            printStackTrace(PrintWriter(sww))
            error(sww.toString())
            error(cause ?: "Unknown cause.")
        }
    }

    fun info(message: Any) = log(DebugType.INFO, message, green)
    fun debug(message: Any) = log(DebugType.DEBUG, message, magenta)
    fun error(message: Any) = log(DebugType.ERROR, message, red)
    fun trace(message: Any) = log(DebugType.TRACE, message, yellow)

    /** Pads the string with the default character of ' ' at the end. */
    private fun padRight(string: String) = string.padEnd(12)

}

/**
 * Created by Mihael Valentin Berčič
 * on 24/10/2021 at 00:49
 * using IntelliJ IDEA
 */
enum class DebugType { ALL, DEBUG, INFO, ERROR, TRACE }