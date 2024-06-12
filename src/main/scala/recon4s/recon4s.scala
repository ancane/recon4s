package recon4s

import com.typesafe.config.{Config, ConfigUtil, ConfigException}
import recon4s.naming.Convention
import recon4s.deriving.Conf
import scala.jdk.CollectionConverters.*
import scala.util.control.NoStackTrace
import scala.concurrent.duration.*
import com.typesafe.config.ConfigFactory

export recon4s.deriving.Configurable
export recon4s.reloading.Reloadable
export recon4s.naming.Convention
export recon4s.naming.Convention.*
export recon4s.reading.ScalaTypes.given
export recon4s.reading.JavaTypes.given

given Convention = CamelToCebabCamelCapsSnake

extension (config: Config)

    def as[T](using c: Configurable[T]): T =
        c.get(Conf(config.atPath(Configurable.rootKey)), Configurable.rootKey)

    def as[T](path: String)(using c: Configurable[T]): T =
        ConfigUtil.splitPath(path).asScala.toVector match
            case keys if keys.size < 1  => throw new ConfigException.BadPath(config.origin(), path) with NoStackTrace
            case keys if keys.size == 1 => c.getValue(Conf(config), keys.head)
            case keys => c.getValue(Conf(config.getConfig(ConfigUtil.joinPath(keys.init*)), keys.init), keys.last)

def watchConfig[T](
    freshConfig: => Config = ConfigFactory.load(),
    reloadInterval: FiniteDuration = 1.minute,
    onReloadSuccess: String => Unit = (_: String) => (),
    onReloadFailure: Throwable => Unit = (_: Throwable) => ()
)(using c: Configurable[T]): T =
    val result = freshConfig.as[T]
    Reloadable.watch(
      interval = reloadInterval,
      freshConfig = freshConfig,
      onSuccess = onReloadSuccess,
      onFailure = onReloadFailure
    )
    result

def watchConfigPath[T](
    path: String
)(
    freshConfig: => Config = ConfigFactory.load(),
    reloadInterval: FiniteDuration = 1.minute,
    onReloadSuccess: String => Unit = (_: String) => (),
    onReloadFailure: Throwable => Unit = (_: Throwable) => ()
)(using c: Configurable[T]): T =
    val result = freshConfig.as[T](path)
    Reloadable.watch(
      interval = reloadInterval,
      freshConfig = freshConfig,
      onSuccess = onReloadSuccess,
      onFailure = onReloadFailure
    )
    result

def stopWatchingConfig(): Unit = Reloadable.stopWatching()
