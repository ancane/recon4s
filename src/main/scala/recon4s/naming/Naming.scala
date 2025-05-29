package recon4s.naming

trait Naming:
    def parse(key: String): List[String]
    def format(keys: List[String]): String

object DashCase extends Naming:
    private val separator = "-"

    def parse(key: String): List[String]  = splitByType(key).filterNot(_ == separator)
    def format(keys: List[String]): String = keys.map(_.toLowerCase()).mkString(separator)

object CamelCase extends Naming:
    def parse(key: String): List[String] = splitByType(key)
    def format(keys: List[String]): String = keys match
        case Nil          => ""
        case head :: Nil  => head.toLowerCase()
        case head :: tail => (head.toLowerCase() :: tail.map(_.toLowerCase.capitalize)).mkString

object CamelCaps extends Naming:
    def parse(key: String): List[String]  = splitByType(key)
    def format(keys: List[String]): String = keys.map(_.toLowerCase.capitalize).mkString

object SnakeCase extends Naming:
    private val separator = "_"

    def parse(key: String): List[String]  = splitByType(key).filterNot(_ == separator)
    def format(keys: List[String]): String = keys.map(_.toLowerCase()).mkString(separator)

private def splitByType(str: String): List[String] =
    import scala.collection.mutable.ListBuffer

    if str.isBlank then Nil
    else
        val charr       = str.toCharArray
        val list        = ListBuffer[String]()
        var tokenStart  = 0
        var currentType = Character.getType(charr(tokenStart))
        for pos <- Range(tokenStart + 1, charr.length) do
            val typ = Character.getType(charr(pos))
            if typ != currentType then
                if typ == Character.LOWERCASE_LETTER && currentType == Character.UPPERCASE_LETTER then
                    val newTokenStart = pos - 1
                    if newTokenStart != tokenStart then
                        list.append(new String(charr, tokenStart, newTokenStart - tokenStart))
                        tokenStart = newTokenStart
                else
                    list.append(new String(charr, tokenStart, pos - tokenStart));
                    tokenStart = pos

                currentType = typ
        list.append(new String(charr, tokenStart, charr.length - tokenStart))
        list.result()
