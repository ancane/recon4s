package recon4stest

import recon4s.{*, given}
import com.typesafe.config.*
import scala.concurrent.duration.*
import java.util.concurrent.TimeUnit
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ScalaTypesSuite extends munit.FunSuite:

    test("recon4s should read basic types") {
        val intV = ConfigFactory
            .parseString("int = 42")
            .as[Int]("int")
        assertEquals(intV, 42)

        val byteV = ConfigFactory
            .parseString("{ byte = 42 }")
            .as[Byte]("byte")
        assertEquals(byteV, 42.toByte)

        val shortV = ConfigFactory
            .parseString("{ short = 42 }")
            .as[Short]("short")
        assertEquals(shortV, 42.toShort)

        val charV = ConfigFactory
            .parseString("{ char = A }")
            .as[Char]("char")
        assertEquals(charV, 'A')

        val longV = ConfigFactory
            .parseString("{ long = 42 }")
            .as[Long]("long")
        assertEquals(longV, 42L)

        val stringV = ConfigFactory
            .parseString("{ string = 42 }")
            .as[String]("string")
        assertEquals(stringV, "42")

        val booleanV = ConfigFactory
            .parseString("{ boolean = no }")
            .as[Boolean]("boolean")
        assertEquals(booleanV, false)

        val doubleV = ConfigFactory
            .parseString("{ double = 42.0 }")
            .as[Double]("double")
        assertEquals(doubleV, 42.0)

        val floatV = ConfigFactory
            .parseString("{ float = 42.0 }")
            .as[Float]("float")
        assertEquals(floatV, 42.0f)

        val bigInt = ConfigFactory
            .parseString("{ bigInt = 9999999999999999999999999999999 }")
            .as[BigInt]("bigInt")
        assertEquals(bigInt, BigInt("9999999999999999999999999999999"))

        val bigDecimal = ConfigFactory
            .parseString("{ big-decimal = 9999999999999999999999999999999.9999999999999999999999999999999 }")
            .as[BigDecimal]("bigDecimal")
        assertEquals(
          bigDecimal,
          BigDecimal("9999999999999999999999999999999.9999999999999999999999999999999")
        )
    }

    test("recon4s should read a bit more complex types") {
        val maybeString = ConfigFactory
            .parseString("{ string = 42 }")
            .as[Option[String]]("string")
        assertEquals(maybeString, Some("42"))

        val setString = ConfigFactory
            .parseString("{ string = [42, 42] }")
            .as[Set[String]]("string")
        assertEquals(setString, Set("42"))

        val vectorString = ConfigFactory
            .parseString("{ string = [42, 35] }")
            .as[Vector[Int]]("string")
        assertEquals(vectorString, Vector(42, 35))

        val duration = ConfigFactory
            .parseString("{ duration = 5 minutes }")
            .as[FiniteDuration]("duration")
        assertEquals(duration, 5.minutes)

        val mapV = ConfigFactory.parseString(
          """|{
             |  map = {
             |    "key.0" : "value"
             |    "key.1" : 34
             |    "key.2" : yes
             |  }
             |}""".stripMargin
        ).as[Map[String, String]]("map")
        assertEquals(mapV, Map("key.0" -> "value", "key.1" -> "34", "key.2" -> "yes"))
    }

    test("recon4s should read an array") {
        case class Arr(arr: Array[Int])
        val actual = ConfigFactory
            .parseString("{ arr = [1, -1] }")
            .as[Arr]

        assert(actual.arr.sameElements(Array(1, -1)))
    }

    test("recon4s should read tuple from a list") {
        case class Tpl(tuple: (String, Int, Boolean))
        val actual = ConfigFactory
            .parseString("{ tuple = [Str, 1, false] }")
            .as[Tpl]
        assertEquals(actual.tuple, ("Str", 1, false))
    }

    test("recon4s should override givens") {
        given (using c: Configurable[String]): Configurable[String] =
            c.map { st => s"$st-modified" }

        case class StringOverride(option: String)
        val actual = ConfigFactory
            .parseString("{ option = Some }")
            .as[StringOverride]

        assertEquals(actual.option, "Some-modified")
    }

    test("recon4s should consider provided givens") {
        given (using c: Configurable[String]): Configurable[LocalDate] =
            c.map(LocalDate.parse(_, DateTimeFormatter.ofPattern("yyyy/MM/dd")))

        case class LocalDateTest(date: LocalDate)

        val actual = ConfigFactory
            .parseString("date = 2007/12/03")
            .as[LocalDateTest]

        assertEquals(actual.date, LocalDate.parse("2007-12-03"))
    }

    test("recon4s should read configs recursively") {
        case class Recur(x: Option[Recur]) derives Configurable
        val actual = ConfigFactory
            .parseString("{ x = { x = {} }}")
            .as[Recur]

        assertEquals(actual, Recur(Some(Recur(Some(Recur(None))))))
    }

    test("recon4s should read class fields and format to config keys") {
        import naming.*

        val actual = CamelCase.parse("classFieldNAME20millis50Miles")
        assertEquals(actual, List("class", "Field", "NAME", "20", "millis", "50", "Miles"))

        val camel = CamelCase.format(actual)
        assertEquals(camel, "classFieldName20Millis50Miles")

        val camelCaps = CamelCaps.format(actual)
        assertEquals(camelCaps, "ClassFieldName20Millis50Miles")

        val cebab = CebabCase.format(actual)
        assertEquals(cebab, "class-field-name-20-millis-50-miles")

        val snake = SnakeCase.format(actual)
        assertEquals(snake, "class_field_name_20_millis_50_miles")
    }

    test("recon4s should read camel case keys") {
        enum Color(val rgb: Int) derives Configurable:
            case Red   extends Color(0xff0000)
            case Green extends Color(0x00ff00)
            case Blue  extends Color(0x0000ff)

        case class CamelCase(camelCase: Color)
        val actual = ConfigFactory
            .parseString("{ camel-case = green }")
            .as[CamelCase]

        assertEquals(actual, CamelCase(Color.Green))
    }

    test("recon4s should read snake case keys") {
        given Convention = recon4s.CamelToCebabCamelCapsSnake

        enum Color(val rgb: Int):
            case Red   extends Color(0xff0000)
            case Green extends Color(0x00ff00)
            case Blue  extends Color(0x0000ff)

        case class CamelCase(camelCase: Color)

        val actual = ConfigFactory
            .parseString("{ camel_case = Blue }")
            .as[CamelCase]

        assertEquals(actual, CamelCase(Color.Blue))
    }

    test("recon4s should read nested classes from cebab keys") {
        given Convention = recon4s.CamelToCebabCamelCaps

        enum Color(val rgb: Int):
            case Red   extends Color(0xff0000)
            case Green extends Color(0x00ff00)
            case Blue  extends Color(0x0000ff)

        case class Nested(nestedCase: Color)
        case class Camel(camelCase: Nested)
        val actual = ConfigFactory
            .parseString("{ camel-case = { nestedCase = Red }}")
            .as[Camel]

        assertEquals(actual, Camel(Nested(Color.Red)))
    }

    test("recon4s should allow substituting class field for config keys") {
        given Convention = recon4s.CamelToCebab.substitute("one", "TWO")

        case class Conf(one: String)
        val actual = ConfigFactory
            .parseString("{ TWO = 1}")
            .as[Conf]

        assertEquals(actual, Conf("1"))
    }

    test("recon4s should read config as map") {
        case class DB(connectionPoints: Set[String], connectionTimeout: FiniteDuration, tls: Option[Boolean])

        val actual = ConfigFactory.parseString(
          """
            |{
            |  cassandra {
            |    connectionPoints = ["localhost"]
            |    connection-timeout = 10 seconds
            |    tls = true
            |  }
            |  mysql {
            |    connectionPoints = ["localhost:8092", "localhost:8093"]
            |    connection-timeout = 500 millis
            |  }
            |}""".stripMargin
        ).as[Map[String, DB]]

        assertEquals(
          actual,
          Map(
            "cassandra" -> DB(Set("localhost"), FiniteDuration(10, TimeUnit.SECONDS), Some(true)),
            "mysql"     -> DB(Set("localhost:8092", "localhost:8093"), FiniteDuration(500, TimeUnit.MILLISECONDS), None)
          )
        )
    }

    test("recon4s should provide naming convention mappings") {
        assertEquals(
          CamelToCebab.variants("camelCase"),
          Vector("camel-case", "camelCase")
        )
        assertEquals(
          CamelToCebabCamel.variants("camelCase"),
          Vector("camel-case", "camelCase")
        )
        assertEquals(
          CamelToCebabCamelCaps.variants("camelCase"),
          Vector("camel-case", "camelCase", "CamelCase")
        )
        assertEquals(
          CamelToCebabCamelCapsSnake.variants("camelCase"),
          Vector("camel-case", "camelCase", "CamelCase", "camel_case")
        )
    }

    test("recon4s should read descriminator on sealed families") {
        sealed trait Shape
        case class CircleShape(radius: Int) extends Shape
        case class Square(side: Int)        extends Shape

        case class Test(shape: Shape)

        val actual0 = ConfigFactory
            .parseString("{shape = { type = CircleShape, radius = 42 }}")
            .as[Test]

        assertEquals(actual0.shape, CircleShape(42))

        val actual1 = ConfigFactory
            .parseString("{shape = { type = circle-shape, radius = 42 }}")
            .as[Test]

        assertEquals(actual1.shape, CircleShape(42))

        val actual2 = ConfigFactory
            .parseString("{shape = { type = circleShape, radius = 42 }}")
            .as[Test]

        assertEquals(actual2.shape, CircleShape(42))
    }

    test("recon4s should read custom descriminator") {
        given Convention = CamelToCebabCamel.withDescriminaton("name")

        sealed trait Shape
        case class CircleShape(radius: Int) extends Shape
        case class Square(side: Int)        extends Shape

        case class Test(shape: Shape)

        val actual0 = ConfigFactory
            .parseString("{shape = { name = CircleShape, radius = 42 }}")
            .as[Test]

        assertEquals(actual0.shape, CircleShape(42))

        val actual1 = ConfigFactory
            .parseString("{shape = { name = circle-shape, radius = 42 }}")
            .as[Test]

        assertEquals(actual1.shape, CircleShape(42))

        val actual2 = ConfigFactory
            .parseString("{shape = { name = circleShape, radius = 42 }}")
            .as[Test]

        assertEquals(actual2.shape, CircleShape(42))
    }

    test("recon4s should consider default values") {
        case class CircleShape(radius: Int = 1)

        val actual = ConfigFactory
            .parseString("{}")
            .as[CircleShape]

        assertEquals(actual, CircleShape(1))
    }

    test("recon4s should consider nested default values") {
        case class Nested(nestedCase: Int = 42)
        case class Camel(camelCase: Nested = Nested(21))
        val actual = ConfigFactory
            .parseString("{ camel-case = { }}")
            .as[Camel]

        assertEquals(actual, Camel(Nested(42)))
    }

    test("recon4s should consider default product values") {
        case class Nested(nestedCase: Int)
        case class Camel(camelCase: Nested = Nested(21))
        val actual = ConfigFactory
            .parseString("{ camel-case = { }}")
            .as[Camel]

        assertEquals(actual, Camel(Nested(21)))
    }

    test("recon4s should read enums with convention override") {
        import recon4s.naming.*
        given Convention = Convention(
          from = CebabCase,
          to = Vector(CamelCase, CamelCaps)
        )
        enum Color(val rgb: Int) derives Configurable:
            case Red   extends Color(0xff0000)
            case Green extends Color(0x00ff00)
            case Blue  extends Color(0x0000ff)

        case class Test(`color-value`: Color)
        val color = ConfigFactory
            .parseString("{ colorValue = red }")
            .as[Test]
        assertEquals(color.`color-value`, Color.Red)
    }

    test("recon4s should read enums with naming override") {
        import recon4s.naming.*
        given Convention = Convention(
          from = CebabCase,
          to = Vector(CamelCase, CamelCaps)
        )

        enum Color(val rgb: Int) derives Configurable:
            case Red             extends Color(0xff0000)
            case Green           extends Color(0x00ff00)
            case Blue(cmyk: Int) extends Color(cmyk)

        case class Test(`color-value`: Color)
        val red = ConfigFactory
            .parseString("{ colorValue = red }")
            .as[Test]
        assertEquals(red, Test(Color.Red))

        val blue = ConfigFactory
            .parseString("{ colorValue = { type = blue, cmyk = 0 } }")
            .as[Test]
        assertEquals(blue, Test(Color.Blue(0)))
    }

    test("recon4s should read trait family") {
        case class XY(x: Int, y: Int)

        sealed trait Shape
        case class SquareBlock(side: Int)    extends Shape
        case class RectangleBlock(sides: XY) extends Shape

        case class Test(shape: Shape)

        val actual = ConfigFactory
            .parseString("{ shape = { type = rectangle-block, sides = { x = 1, y = 2} }}")
            .as[Test]
        assertEquals(actual, Test(RectangleBlock(XY(1, 2))))
    }

    test("recon4s should read cebab enum name") {
        enum Shape(val x: Int):
            case CircleShape       extends Shape(42)
            case Square(side: Int) extends Shape(side)

        case class Test(shape: Shape)

        val actual0 = ConfigFactory
            .parseString("{ shape = circle-shape }")
            .as[Test]

        assertEquals(actual0, Test(Shape.CircleShape))

        val actual1 = ConfigFactory
            .parseString("{ shape = circleShape }")
            .as[Test]

        assertEquals(actual1, Test(Shape.CircleShape))
    }

    test("basic example") {
        import com.typesafe.config.*
        import recon4s.{*, given}

        case class AppConf(
            appName: String,
            appVersion: String = "0.1",
            snakeBites: Boolean
        ) derives Configurable

        val config = ConfigFactory.parseString(
          """|
             |    appName = recon4s
             |    app-version = "1.2.3"
             |    snake_bites = yes
             |
             |""".stripMargin
        )

        val appConfig = config.as[AppConf]
        assertEquals(appConfig.appName, "recon4s")
        assertEquals(appConfig.appVersion, "1.2.3")
        assert(appConfig.snakeBites)
    }

end ScalaTypesSuite
