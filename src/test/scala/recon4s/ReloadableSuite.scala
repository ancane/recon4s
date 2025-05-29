package recon4s

import recon4s.{*, given}
import com.typesafe.config.*
import recon4s.deriving.Configurable
import java.nio.file.*
import scala.concurrent.Promise
import scala.concurrent.duration.*

class ReloadableSuite extends munit.FunSuite:
    given ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

    case class Testr(fieldValue: Reloadable[Int])
    case class TestBoolean(active: Reloadable[Boolean])

    private val file = new Fixture[Path]("files"):
        private var temp: Path = null
        def apply() = temp

        override def beforeEach(context: BeforeEach): Unit =
            temp = Files.createTempFile("recon4s", ".conf")

        override def afterEach(context: AfterEach): Unit =
            Files.deleteIfExists(temp)
            Reloadable.stopWatching()

    override def munitFixtures = List(file)

    test("recon4s should derive reloadable as class member") {
        case class Test(value: Reloadable[Int]) derives Configurable

        val actual = ConfigFactory
            .parseString("{ value = 42 }")
            .as[Test]

        assertEquals(actual.value.get(), 42)
    }

    test("recon4s should derive reloadable directly") {
        val actual = ConfigFactory
            .parseString("{ value = 42 }")
            .as[Reloadable[Int]]("value")

        assertEquals(actual.get(), 42)
    }

    test("recon4s should reload value from included file (specific root)") {
        val result = Promise[String]()
        val path   = file()
        write(path, "app.name { fieldValue = 42 }")

        val actual = watchConfigPath[Testr]("app.name")(
          freshConfig = ConfigFactory.parseString(s"""{ include file("$path") }"""),
          reloadInterval = 200.millis,
          onReloadSuccess = path => result.success(path)
        )

        assertEquals(actual.fieldValue.get(), 42)

        write(path, "app.name { fieldValue = 46 }")

        result.future.foreach { _ =>
            assertEquals(actual.fieldValue.get(), 46)
        }
    }

    test("recon4s should reload value from included file (no root path provided)") {
        val result = Promise[String]()
        val path   = file()
        write(path, "{ fieldValue = 42 }")

        val actual = watchConfig[Testr](
          freshConfig = ConfigFactory.parseString(s"""{ include file("$path") }"""),
          reloadInterval = 200.millis,
          onReloadSuccess = path => result.success(path)
        )

        assertEquals(actual.fieldValue.get(), 42)
        write(path, "{ fieldValue = 46 }")

        result.future.foreach { _ =>
            assertEquals(actual.fieldValue.get(), 46)
        }
    }

    test("recon4s should reload value from renamed key (migration case)") {
        val result = Promise[String]()
        val path   = file()
        write(path, "{ fieldValue = 42 }")

        val actual = watchConfig[Testr](
          freshConfig = ConfigFactory.parseString(s"""{ include file("$path") }"""),
          reloadInterval = 200.millis,
          onReloadSuccess = path => result.success(path)
        )

        assertEquals(actual.fieldValue.get(), 42)

        write(path, "{ field-value = 46 }")

        result.future.foreach { _ =>
            assertEquals(actual.fieldValue.get(), 46)
        }
    }

    test("recon4s should watch config changes") {
        val result = Promise[String]()
        val path   = file()
        write(path, "{ fieldValue = 42 }")

        val actual = watchConfig[Testr](
          freshConfig = ConfigFactory.parseString(s"""{ include file("$path") }"""),
          reloadInterval = 200.millis,
          onReloadSuccess = path => result.success(path)
        )

        assertEquals(actual.fieldValue.get(), 42)

        write(path, "{ fieldValue = 46 }")

        result.future.foreach { _ =>
            assertEquals(actual.fieldValue.get(), 46)
        }
    }

    test("recon4s should watch single value") {
        val result = Promise[String]()
        val path   = file()
        write(path, "{ fieldValue = 42 }")

        val actual = watchConfigPath[Reloadable[Int]]("fieldValue")(
          freshConfig = ConfigFactory.parseString(s"""{ include file("$path") }"""),
          reloadInterval = 200.millis,
          onReloadSuccess = path => result.success(path)
        )

        assertEquals(actual.get(), 42)

        write(path, "{ fieldValue = 46 }")

        result.future.foreach { _ =>
            assertEquals(actual.get(), 46)
        }
    }

    test("recon4s should not crash while reading fresh config and report the failure") {
        val result = Promise[String]()
        val path   = Files.createTempFile("recon4s", ".conf")

        write(path, "{ fieldValue = 42 }")

        val actual = watchConfig[Testr](
          freshConfig = ConfigFactory.parseString(s"""{ include file("$path") }"""),
          reloadInterval = 200.millis,
          onReloadFailure = e => result.failure(e),
          onReloadSuccess = path => result.success(path)
        )

        assertEquals(actual.fieldValue.get(), 42)

        write(path, "{ fieldValue = 46 ")

        assertEquals(actual.fieldValue.get(), 42)

        result.future.failed.map { e =>
            assert(e.getMessage().contains("Expecting close brace }"), e.getMessage())
        }
    }

    test("recon4s should not fail reading reloadable value and report the failure") {
        val result = Promise[String]()
        val path   = Files.createTempFile("recon4s", ".conf")
        write(path, "{ active = yes }")

        val actual = watchConfig[TestBoolean](
          freshConfig = ConfigFactory.parseString(s"""{ include file("$path") }"""),
          reloadInterval = 200.millis,
          onReloadFailure = e => result.failure(e),
          onReloadSuccess = path => result.success(path)
        )

        assertEquals(actual.active.get(), true)

        write(path, "{ active = nope }")

        assertEquals(actual.active.get(), true)

        result.future.failed.map { e =>
            assert(e.getMessage().contains("has type STRING rather than BOOLEAN"), e.getMessage())
        }
    }

    private def write(path: Path, content: String): Unit =
        val fwr = Files.newBufferedWriter(path, StandardOpenOption.TRUNCATE_EXISTING)
        fwr.write(content)
        fwr.flush()
        fwr.close()

end ReloadableSuite
