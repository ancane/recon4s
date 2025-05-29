package recon4s.reading

import com.typesafe.config.ConfigException
import recon4s.naming.Convention
import scala.util.matching.Regex
import scala.concurrent.duration.{Duration, FiniteDuration, NANOSECONDS}
import scala.collection.Factory
import recon4s.deriving.{Conf, Config, Configurable}
import recon4s.reloading.Reloadable

private[recon4s] trait ScalaTypes:

    inline given Configurable[Byte] with
        def get(config: Config, key: String): Byte =
            val num = config.getInt(key)
            if num.isValidByte
            then num.toByte
            else throw ConfigException.WrongType(config.origin(), s"Not a Byte at $key")

    inline given Configurable[Short] with
        def get(config: Config, key: String): Short =
            val num = config.getInt(key)
            if num.isValidShort
            then num.toShort
            else throw ConfigException.WrongType(config.origin(), s"Not a Short at $key")

    inline given Configurable[Char] with
        def get(config: Config, key: String): Char =
            config.getString(key) match
                case str if str.length() == 1 => str(0)
                case str => throw ConfigException.WrongType(config.origin(), s"Not a Char at $key")

    inline given Configurable[Int] with
        def get(config: Config, key: String): Int = config.getInt(key)

    inline given Configurable[Long] with
        def get(config: Config, key: String): Long = config.getLong(key)

    inline given Configurable[String] with
        def get(config: Config, key: String): String = config.getString(key)

    inline given Configurable[Boolean] with
        def get(config: Config, key: String): Boolean = config.getBoolean(key)

    inline given Configurable[Double] with
        def get(config: Config, key: String): Double = config.getDouble(key)

    inline given Configurable[Float] with
        def get(config: Config, key: String): Float = config.getDouble(key).toFloat

    inline given Configurable[Regex] with
        def get(config: Config, key: String): Regex = Regex(config.getString(key))

    inline given Configurable[BigInt] with
        def get(config: Config, key: String): BigInt = BigInt(config.getString(key))

    inline given Configurable[BigDecimal] with
        def get(config: Config, key: String): BigDecimal = BigDecimal(config.getString(key))

    inline given Configurable[FiniteDuration] with
        def get(config: Config, key: String): FiniteDuration =
            Duration.fromNanos(config.getDuration(key, NANOSECONDS))

    inline given reloadable[T: Configurable]: Configurable[Reloadable[T]] = new ConfigurableReloadable[T]

    class ConfigurableReloadable[T: Configurable as c] extends Configurable[Reloadable[T]]:
        def get(config: Config, key: String): Reloadable[T] =
            val instance = new Reloadable(c) {}
            instance.get(config, key)
            Reloadable.register(config, key, instance)
            instance

    inline given optional[T](using c: Configurable[T]): Configurable[Option[T]] with
        override def getValue(config: Config, key: String)(using naming: Convention): Option[T] =
            naming.variants(key)
                .collectFirst { case variant if config.hasKey(variant) => get(config, variant) }
                .getOrElse(None)

        def get(config: Config, key: String): Option[T] =
            if config.hasKey(key)
            then Option(c.get(config, key))
            else None

    inline given collections[C[_], T](using c: Configurable[T], factory: Factory[T, C[T]]): Configurable[C[T]] with
        def get(config: Config, key: String): C[T] =
            val values = config.getList(key).zipWithIndex.map { (value, i) =>
                val valueKey = s"$key-$i"
                val cfg       = Conf(value.atPath(valueKey), config.keys)
                c.get(cfg, valueKey)
            }
            val builder = factory.newBuilder
            builder.addAll(values)
            builder.result()

    inline given maps[T: Configurable]: Configurable[Map[String, T]] = new ConfigurableMap[T]

    class ConfigurableMap[T: Configurable as c] extends Configurable[Map[String, T]]:
        import scala.jdk.CollectionConverters.given

        def get(config: Config, key: String): Map[String, T] =
            val cfg = config.getConfig(key)
            cfg.underlying
                .root().entrySet().asScala
                .map { entry =>
                    val key   = entry.getKey
                    val value = c.get(cfg, key)
                    key -> value
                }.toMap

end ScalaTypes

object ScalaTypes extends ScalaTypes
