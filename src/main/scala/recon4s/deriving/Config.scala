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
    def hasKey(key: String): Boolean       = underlying.root().keySet().contains(key)
    def origin()                           = underlying.origin()
    def getValue(key: String): ConfigValue = catchErrors(key, _.getValue(key))
    def getNonEmpty(key: String): ConfigValue =
        val v = underlying.root().get(key)
        if v == null then throw new ConfigException.Missing(origin(), completePath(key)) with NoStackTrace
        else v
    def getString(key: String): String           = catchErrors(key, _.getString(key))
    def getList(key: String): List[ConfigValue]  = catchErrors(key, _.getList(key)).asScala.toList
    def getConfig(key: String): Config           = Conf(catchErrors(key, _.getConfig(key)), keys :+ key)
    def getInt(key: String): Int                 = catchErrors(key, _.getInt(key))
    def getLong(key: String): Long               = catchErrors(key, _.getLong(key))
    def getBoolean(key: String): Boolean         = catchErrors(key, _.getBoolean(key))
    def getDouble(key: String): Double           = catchErrors(key, _.getDouble(key))
    def getDuration(key: String): Duration       = catchErrors(key, _.getDuration(key))
    def getDuration(key: String, unit: TimeUnit) = catchErrors(key, _.getDuration(key, unit))
    def getPeriod(key: String): Period           = catchErrors(key, _.getPeriod(key))
    def atPath(value: ConfigValue, key: String): Config = Conf(
      underlying = value.atPath(key),
      keys = keys :+ key
    )

    private def catchErrors[T](key: String, f: TypesafeConfig => T): T =
        val cfg = getNonEmpty(key).atPath(key)
        Try(f(cfg)).recover {

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
