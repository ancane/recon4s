package recon4s.reloading

import com.typesafe.config.{Config as TypesafeConfig, ConfigFactory, ConfigUtil}
import java.util.concurrent.atomic.AtomicReference
import java.util.{Timer, TimerTask}
import recon4s.deriving.{Configurable, Config, Conf}
import recon4s.naming.Convention
import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.*
import scala.util.{Failure, Success, Try}

trait Reloadable[T](c: Configurable[T]) extends Configurable[T]:
    self =>

    private val valueRef = new AtomicReference[T]()

    def get(): T = valueRef.get()
    override def get(config: Config, key: String): T =
        val value = c.get(config, key)
        valueRef.set(value)
        value

    override def getValue(config: Config, key: String)(using naming: Convention): T =
        val value = c.getValue(config, key)
        valueRef.set(value)
        value

object Reloadable:
    private val configs = TrieMap[String, (Config, String, Reloadable[?])]()
    private val timer   = AtomicReference[Timer]()

    private[recon4s] def register(config: Config, key: String, instance: Reloadable[?]): Unit =
        configs.putIfAbsent(config.completePath(key), (config, key, instance))
        ()

    private def reload(
        refreshConfig: => TypesafeConfig,
        onSuccess: String => Unit = (_: String) => (),
        onFailure: Throwable => Unit = _ => ()
    )(using Convention): Unit =
        Try {
            ConfigFactory.invalidateCaches()
            refreshConfig
        } match
            case Success(freshConfig) =>
                configs.values.foreach { case (cfg @ Conf(_, keys), key, reloadable) =>
                    val effectiveKeys = keys.filterNot(_ == Configurable.rootKey)
                    val config =
                        if effectiveKeys.isEmpty
                        then freshConfig
                        else freshConfig.getConfig(ConfigUtil.joinPath(effectiveKeys*))

                    Try(reloadable.getValue(Conf(config, effectiveKeys), key)) match
                        case Failure(ex)    => onFailure(ex)
                        case Success(value) => onSuccess(cfg.completePath(key))
                }
            case Failure(ex) => onFailure(ex)

    private[recon4s] def watch(
        freshConfig: => TypesafeConfig,
        interval: FiniteDuration = 1.minute,
        onSuccess: String => Unit = (_: String) => (),
        onFailure: Throwable => Unit = _ => ()
    )(using Convention): Unit =
        val prevTimer = timer.getAndSet {
            val tmr = new Timer()
            tmr.schedule(
              new TimerTask:
                  def run() = reload(freshConfig, onSuccess, onFailure)
              ,
              interval.toMillis,
              interval.toMillis
            )
            tmr
        }
        Option(prevTimer).foreach(_.cancel())

    private[recon4s] def stopWatching() =
        Option(timer.get()).foreach(_.cancel())
        configs.clear()

end Reloadable
