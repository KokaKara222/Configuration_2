package depgraph

import java.io.PrintStream
import kotlin.system.exitProcess

const val DEFAULT_CONFIG_PATH = "config.xml"
const val EXIT_OK = 0
const val EXIT_CONFIG_ERROR = 1
const val EXIT_USAGE_ERROR = 2

private const val MAX_ARGUMENTS = 1
private val HELP_FLAGS = setOf("-h", "--help")

fun main(args:Array<String>) {
    val exitCode = runApp(args, System.out, System.err)
    if (exitCode != 0) exitProcess(exitCode)
}

fun runApp(args:Array<String>, out: PrintStream, err:PrintStream): Int{
    if(args.any {it in HELP_FLAGS}){
        printUsage(out)
        return EXIT_OK
    }
    if (args.size > MAX_ARGUMENTS) {
        err.println("Ошибка: слишком много аргументов командной строки")
        printUsage(err)
        return EXIT_USAGE_ERROR
    }
    return try{
        val config = ConfigLoader.load(args.firstOrNull() ?: DEFAULT_CONFIG_PATH)
        config.toKeyValue().forEach { (key, value) -> out.println("$key: $value") }
        EXIT_OK
    } catch(e:ConfigException){
        err.println("Ошибка конфигурации: ${e.message}")
        EXIT_CONFIG_ERROR
    }
}

private fun printUsage(stream: PrintStream) {
    stream.println("Использование: depgraph [путь/к/config.xml]")
    stream.println("Если путь не указан, используется ./$DEFAULT_CONFIG_PATH")
}
