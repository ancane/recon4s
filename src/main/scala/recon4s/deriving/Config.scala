package recon4s.deriving

import com.typesafe.config.{Config as TypesafeConfig, ConfigValue, ConfigUtil}
import scala.jdk.CollectionConverters.given
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.time.Period
import com.typesafe.config.ConfigException
import scala.util.control.NoStackTrace
import scala.util.Try

sealed trait Config:
    def keys: Vector[String]
    def underlying: TypesafeConfig
    def completePath(key: String): String =
        val pathElems = keys.filterNot(_ == Configurable.rootKey) :+ key
        ConfigUtil.joinPath(pathElems*)
    def hasKey(key: String): Boolean             = catchErrors(key, underlying.root().keySet().contains)
    def origin()                                 = underlying.origin()
    def getValue(key: String): ConfigValue       = catchErrors(key, underlying.getValue)
    def getString(key: String): String           = catchErrors(key, underlying.getString)
    def getList(key: String): List[ConfigValue]  = catchErrors(key, underlying.getList).asScala.toList
    def getConfig(key: String): Config           = Conf(catchErrors(key, underlying.getConfig), keys :+ key)
    def getInt(key: String): Int                 = catchErrors(key, underlying.getInt)
    def getLong(key: String): Long               = catchErrors(key, underlying.getLong)
    def getBoolean(key: String): Boolean         = catchErrors(key, underlying.getBoolean)
    def getDouble(key: String): Double           = catchErrors(key, underlying.getDouble)
    def getDuration(key: String): Duration       = catchErrors(key, underlying.getDuration)
    def getDuration(key: String, unit: TimeUnit) = catchErrors(key, { k => underlying.getDuration(k, unit) })
    def getPeriod(key: String): Period           = catchErrors(key, underlying.getPeriod)
    def atPath(value: ConfigValue, key: String): Config = Conf(
      underlying = value.atPath(key),
      keys = keys :+ key
    )

    private def catchErrors[T](key: String, f: String => T): T =
        Try(f(key)).recover {

            case _: ConfigException.Missing =>
                throw new ConfigException.Missing(origin(), completePath(key)) with NoStackTrace

            case e: ConfigException.BadValue =>
                throw new ConfigException.BadValue(origin(), completePath(key), e.getMessage()) with NoStackTrace

            case e: ConfigException.WrongType =>
                throw new ConfigException.WrongType(
                  origin(),
                  s"Wrong type at '${completePath(key)}', ${e.getMessage()}"
                ) with NoStackTrace
        }.get

private[recon4s] final case class Conf(
    underlying: TypesafeConfig,
    keys: Vector[String] = Vector.empty
) extends Config
