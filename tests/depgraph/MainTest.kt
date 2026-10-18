package depgraph

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MainTest {

    private class Result(val code: Int, val out: String, val err: String)

    private fun run(vararg args: String): Result {
        val out = ByteArrayOutputStream()
        val err = ByteArrayOutputStream()
        val code = runApp(arrayOf(*args), PrintStream(out, true), PrintStream(err, true))
        return Result(code, out.toString(), err.toString())
    }

    private fun configFile(content: String): File {
        val file = Files.createTempFile("depgraph", ".xml").toFile()
        file.deleteOnExit()
        file.writeText(content)
        return file
    }

    @Test
    fun printsAllParametersAsKeyValue() {
        val file = configFile(
            "<config><package_name>serde</package_name>" +
                    "<repository>https://crates.io</repository><mode>remote</mode></config>"
        )
        val result = run(file.path)
        assertEquals(EXIT_OK, result.code)
        assertTrue(result.out.contains("package_name: serde"))
        assertTrue(result.out.contains("repository: https://crates.io"))
        assertTrue(result.out.contains("mode: remote"))
    }

    @Test
    fun returnsConfigErrorCode() {
        val result = run("no/such/file.xml")
        assertEquals(EXIT_CONFIG_ERROR, result.code)
        assertTrue(result.err.contains("Ошибка конфигурации"))
    }

    @Test
    fun returnsUsageErrorOnTooManyArguments() {
        val result = run("a.xml", "b.xml")
        assertEquals(EXIT_USAGE_ERROR, result.code)
    }

    @Test
    fun printsHelp() {
        val result = run("--help")
        assertEquals(EXIT_OK, result.code)
        assertTrue(result.out.contains("Использование"))
    }
}