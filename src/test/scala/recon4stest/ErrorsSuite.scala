package recon4stest

import recon4s.{*, given}
import com.typesafe.config.*
import recon4s.deriving.Configurable
import scala.concurrent.duration.FiniteDuration

class ErrorsSuite extends munit.FunSuite:

    test("recon4s should print complete path when key is missing") {
        case class MissingKey(email: String, missing: String)
        case class MissingTest(registeredMember: MissingKey)

        val config = ConfigFactory.parseString(
          """|{ recon4s.registered-member = {
             |    id = 1234
             |    email = String
             |}}""".stripMargin
        )

        val ex = intercept[ConfigException.Missing] {
            config.as[MissingTest]("recon4s")
        }

        assert(
          ex.getMessage().contains("No configuration setting found for key 'recon4s.registered-member.missing'"),
          ex.getMessage()
        )
    }

    test("recon4s should print complete path when key has wrong type") {
        case class WrontType(email: Int)
        case class WrongTypeTest(registeredMember: WrontType)

        val config = ConfigFactory.parseString(
          """|{    
             |    recon4s.registered-member = {
             |        email = String
             |    }
             |}""".stripMargin
        )

        val ex = intercept[ConfigException.WrongType] {
            config.as[WrongTypeTest]("recon4s")
        }

        assert(ex.getMessage().contains("Wrong type at 'recon4s.registered-member.email'"), ex.getMessage())
        assert(ex.getMessage().contains("email has type STRING rather than NUMBER"), ex.getMessage())
    }

    test("recon4s should print complete path when key has bad value") {
        case class BadValue(wrongDuration: FiniteDuration)
        case class BadValueTest(registeredMember: BadValue)

        val config = ConfigFactory.parseString(
          """|{
             |    recon4s.registered-member = {
             |        wrong-duration = 42 things
             |    }
             |}""".stripMargin
        )

        val ex = intercept[ConfigException.BadValue] {
            config.as[BadValueTest]("recon4s")
        }

        assert(ex.getMessage().contains("Invalid value at 'recon4s.registered-member.wrong-duration'"), ex.getMessage())
        assert(ex.getMessage().contains("Could not parse time unit 'things'"), ex.getMessage())
    }

    test("recon4s should handle null values") {
        case class NullTest(nullable: String)
        val cfg = ConfigFactory.parseString("{ nullable = null }")

        intercept[ConfigException.Missing] {
            cfg.as[NullTest]
        }
    }

    test("recon4s should fail on missing subtype descriminator field") {
        sealed trait Shape
        case class CircleShape(radius: Int) extends Shape
        case class Square(side: Int)        extends Shape

        case class NoSubtype(notAShape: Shape)

        val config = ConfigFactory.parseString("not-a-shape = { side = 35 }")
        val ex = intercept[ConfigException.Missing] {
            config.as[NoSubtype]
        }

        assert(ex.getMessage().contains("No configuration setting found for key 'not-a-shape.type'"), ex.getMessage())
    }

    test("recon4s should fail on missing enum name") {
        enum Color(val rgb: Int) derives Configurable:
            case Red   extends Color(0xff0000)
            case Green extends Color(0x00ff00)
            case Blue  extends Color(0x0000ff)

        case class NotEnum(notColor: Color)

        val config = ConfigFactory.parseString("notColor = Purple")

        intercept[ConfigException.BadValue] {
            config.as[NotEnum]
        }
    }

    test("recon4s should report bad path") {
        intercept[ConfigException.BadPath] {
            ConfigFactory.parseString("{}").as[Int]("")
        }
    }

end ErrorsSuite
