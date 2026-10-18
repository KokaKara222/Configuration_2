package depgraph
import org.w3c.dom.Element
import org.xml.sax.ErrorHandler
import org.xml.sax.SAXException
import org.xml.sax.SAXParseException
import java.io.File
import java.io.IOException
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

private const val ROOT_ELEMENT = "config"
private const val DISALLOW_DOCTYPE = "http://apache.org/xml/features/disallow-doctype-decl"
/** Читает XML-файл конфигурации и возвращает «сырые» значения параметров. */
object XmlConfigReader {

    /**
     * Разбирает [file] и возвращает значения параметров по их именам.
     *
     * @throws ConfigException если XML некорректен, структура неверна
     * или найден неизвестный либо повторяющийся параметр
     */
    fun read(file: File): Map<String, String> {
        val root = parseRoot(file)
        if (root.tagName != ROOT_ELEMENT) {
            throw ConfigException(
                "Корневой элемент должен называться <$ROOT_ELEMENT>, найден <${root.tagName}>"
            )
        }
        return collectParameters(root)
    }

    private fun parseRoot(file: File): Element = try {
        createBuilder().parse(file).documentElement
    } catch (e: SAXParseException) {
        throw ConfigException(
            "Некорректный XML (строка ${e.lineNumber}, столбец ${e.columnNumber}): ${e.message}",
            e,
        )
    } catch (e: SAXException) {
        throw ConfigException("Ошибка разбора XML: ${e.message}", e)
    } catch (e: IOException) {
        throw ConfigException("Ошибка чтения файла конфигурации: ${e.message}", e)
    } catch (e: ParserConfigurationException) {
        throw ConfigException("Ошибка инициализации XML-парсера: ${e.message}", e)
    }

    /** Создаёт парсер с защитой от XXE и с исключениями вместо вывода ошибок в stderr. */
    private fun createBuilder(): DocumentBuilder {
        val factory = DocumentBuilderFactory.newInstance()
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        factory.setFeature(DISALLOW_DOCTYPE, true)
        val builder = factory.newDocumentBuilder()
        builder.setErrorHandler(StrictErrorHandler)
        return builder
    }

    private fun collectParameters(root: Element): Map<String, String> {
        val values = linkedMapOf<String, String>()
        val children = root.childNodes
        for (index in 0 until children.length) {
            val element = children.item(index) as? Element ?: continue
            val name = element.tagName
            if (name !in ConfigKeys.ALL) {
                throw ConfigException(
                    "Неизвестный параметр <$name>. Допустимые: ${ConfigKeys.ALL.joinToString(", ")}"
                )
            }
            if (name in values) throw ConfigException("Параметр <$name> указан более одного раза")
            values[name] = element.textContent.trim()
        }
        return values
    }

    /** Обработчик, превращающий любую ошибку разбора в исключение. */
    private object StrictErrorHandler : ErrorHandler {
        override fun warning(exception: SAXParseException) = Unit
        override fun error(exception: SAXParseException) = throw exception
        override fun fatalError(exception: SAXParseException) = throw exception
    }
}