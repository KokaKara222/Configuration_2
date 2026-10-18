package depgraph

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ConfigLoaderTest {

    private fun tempFile(content: String, suffix: String): File {
        val file = Files.createTempFile("depgraph", suffix).toFile()
        file.deleteOnExit()
        file.writeText(content)
        return file
    }

    private fun xml(packageName: String?, repository: String?, mode: String?): String {
        val parts = listOfNotNull(
            packageName?.let { "<package_name>$it</package_name>" },
            repository?.let { "<repository>$it</repository>" },
            mode?.let { "<mode>$it</mode>" },
        )
        return "<?xml version=\"1.0\"?><config>${parts.joinToString("")}</config>"
    }

    private fun assertError(content: String, fragment: String) {
        val path = tempFile(content, ".xml").path
        val error = assertFailsWith<ConfigException> { ConfigLoader.load(path) }
        assertTrue(error.message.orEmpty().contains(fragment), "Сообщение: ${error.message}")
    }

    @Test
    fun loadsRemoteConfig() {
        val path = tempFile(xml("serde", "https://crates.io", "remote"), ".xml").path
        val config = ConfigLoader.load(path)
        assertEquals(Config("serde", "https://crates.io", RepoMode.REMOTE), config)
    }

    @Test
    fun loadsTestConfig() {
        val repo = tempFile("A -> B", ".txt").path
        val path = tempFile(xml("A", repo, "test"), ".xml").path
        assertEquals(RepoMode.TEST, ConfigLoader.load(path).mode)
    }

    @Test
    fun trimsWhitespaceAroundValues() {
        val path = tempFile(xml("  serde ", " https://crates.io ", " remote "), ".xml").path
        assertEquals("serde", ConfigLoader.load(path).packageName)
    }

    @Test
    fun failsWhenConfigFileMissing() {
        val error = assertFailsWith<ConfigException> { ConfigLoader.load("no/such/file.xml") }
        assertTrue(error.message.orEmpty().contains("не найден"))
    }

    @Test
    fun failsWhenConfigPathIsDirectory() {
        val dir = Files.createTempDirectory("depgraph").toFile()
        dir.deleteOnExit()
        val error = assertFailsWith<ConfigException> { ConfigLoader.load(dir.path) }
        assertTrue(error.message.orEmpty().contains("не является файлом"))
    }

    @Test
    fun failsOnMalformedXml() = assertError("<config><package_name>x</package_name>", "XML")

    @Test
    fun failsOnDoctype() = assertError("<!DOCTYPE config []><config/>", "XML")

    @Test
    fun failsOnWrongRoot() = assertError("<settings/>", "Корневой элемент")

    @Test
    fun failsOnUnknownParameter() =
        assertError("<config><foo>1</foo></config>", "Неизвестный параметр")

    @Test
    fun failsOnDuplicateParameter() = assertError(
        "<config><mode>test</mode><mode>remote</mode></config>",
        "более одного раза",
    )

    @Test
    fun failsOnMissingPackageName() =
        assertError(xml(null, "https://crates.io", "remote"), "package_name")

    @Test
    fun failsOnMissingRepository() = assertError(xml("serde", null, "remote"), "repository")

    @Test
    fun failsOnMissingMode() = assertError(xml("serde", "https://crates.io", null), "mode")

    @Test
    fun failsOnEmptyPackageName() =
        assertError(xml("  ", "https://crates.io", "remote"), "не может быть пустым")

    @Test
    fun failsOnInvalidCargoName() =
        assertError(xml("bad name!", "https://crates.io", "remote"), "Недопустимое имя")

    @Test
    fun failsOnCargoNameStartingWithDigit() =
        assertError(xml("1serde", "https://crates.io", "remote"), "Недопустимое имя")

    @Test
    fun failsOnTooLongName() =
        assertError(
            xml("a".repeat(MAX_PACKAGE_NAME_LENGTH + 1), "https://crates.io", "remote"),
            "слишком длинное",
        )

    @Test
    fun failsOnLowercaseNameInTestMode() {
        val repo = tempFile("A ->", ".txt").path
        assertError(xml("serde", repo, "test"), "большие латинские буквы")
    }

    @Test
    fun failsOnUnknownMode() =
        assertError(xml("serde", "https://crates.io", "fast"), "Недопустимый режим")

    @Test
    fun failsOnNonHttpUrl() =
        assertError(xml("serde", "ftp://crates.io", "remote"), "http://")

    @Test
    fun failsOnMalformedUrl() =
        assertError(xml("serde", "https://bad host", "remote"), "Некорректный URL")

    @Test
    fun failsOnUrlWithoutHost() =
        assertError(xml("serde", "https://", "remote"), "URL")

    @Test
    fun failsWhenTestRepositoryFileMissing() =
        assertError(xml("A", "no/such/repo.txt", "test"), "тестового репозитория не найден")
}