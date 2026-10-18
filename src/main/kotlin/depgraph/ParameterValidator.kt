package depgraph

import java.io.File
import java.net.URI
import java.net.URISyntaxException

const val MAX_PACKAGE_NAME_LENGTH = 64

private val CARGO_NAME_REGEX = Regex("^[A-Za-z][A-Za-z0-9_-]*$")
private val TEST_NAME_REGEX = Regex("^[A-Z]+$")
private val ALLOWED_SCHEMES = setOf("http", "https")

/** Проверка значений параметров конфигурации. */
object ParameterValidator {
    fun packageName(name: String, mode: RepoMode): String {
        if (name.length > MAX_PACKAGE_NAME_LENGTH) {
            throw ConfigException(
                "Имя пакета слишком длинное (максимум $MAX_PACKAGE_NAME_LENGTH символов)"
            )
        }
        val regex = if (mode == RepoMode.TEST) TEST_NAME_REGEX else CARGO_NAME_REGEX
        if (!regex.matches(name)) throw ConfigException(invalidNameMessage(name, mode))
        return name
    }
    /**
     * Преобразует значение режима из XML в [RepoMode].
     *
     * @throws ConfigException если значение не входит в список допустимых
     */
    fun mode(value: String): RepoMode = RepoMode.fromXml(value)
        ?: throw ConfigException(
            "Недопустимый режим '$value'. Допустимые значения: " +
                    RepoMode.entries.joinToString(", ") { it.xmlValue }
        )

    /**
     * Проверяет источник пакетов: URL для [RepoMode.REMOTE]
     * или путь к существующему файлу для [RepoMode.TEST].
     *
     * @throws ConfigException если значение не подходит для выбранного режима
     */
    fun repository(value: String, mode: RepoMode): String = when (mode) {
        RepoMode.REMOTE -> url(value)
        RepoMode.TEST -> readableFile(value, "тестового репозитория")
    }


    private fun invalidNameMessage(name: String, mode: RepoMode): String = when (mode) {
        RepoMode.TEST ->
            "Недопустимое имя пакета '$name': в тестовом режиме разрешены только " +
                    "большие латинские буквы (A-Z)"
        RepoMode.REMOTE ->
            "Недопустимое имя пакета '$name': имя Cargo начинается с латинской буквы " +
                    "и содержит только буквы, цифры, '-' и '_'"
    }

    private fun url(value: String): String {
        val uri = try {
            URI(value)
        } catch (e: URISyntaxException) {
            throw ConfigException("Некорректный URL репозитория '$value': ${e.reason}", e)
        }
        if (uri.scheme?.lowercase() !in ALLOWED_SCHEMES) {
            throw ConfigException(
                "URL репозитория должен начинаться с http:// или https:// (получено: '$value')"
            )
        }
        if (uri.host.isNullOrBlank()) {
            throw ConfigException("В URL репозитория '$value' не указан хост")
        }
        return value
    }
    /** Проверяет, что [path] — существующий читаемый файл; [what] нужен для текста ошибки. */
    fun readableFile(path: String, what: String): String {
        val file = File(path)
        if (!file.exists()) throw ConfigException("Файл $what не найден: $path")
        if (!file.isFile) throw ConfigException("Путь $what не является файлом: $path")
        if (!file.canRead()) throw ConfigException("Нет прав на чтение файла $what: $path")
        return path
    }
}