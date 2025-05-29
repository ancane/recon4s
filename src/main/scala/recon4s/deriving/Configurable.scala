package recon4s.deriving

import com.typesafe.config.{ConfigValue, ConfigException, ConfigValueType}
import recon4s.naming.Convention
import recon4s.reading.*
import scala.compiletime.{constValue, erasedValue, summonInline, summonFrom}
import scala.deriving.*
import scala.util.control.NoStackTrace
import scala.util.Try

trait Configurable[T]:
    self =>

    def get(config: Config, key: String): T

    def getValue(config: Config, key: String)(using naming: Convention): T =
        naming
            .variants(key)
            .collectFirst { case variant if config.hasKey(variant) => get(config, variant) }
            .getOrElse(throw new ConfigException.Missing(config.origin(), config.completePath(key)) with NoStackTrace)

    def map[S](f: T => S): Configurable[S] = new Configurable[S]:
        def get(config: Config, key: String): S = f(self.get(config, key))

object Configurable extends ScalaTypes, JavaTypes:
    private[recon4s] val rootKey = "__root__"

    inline given derived[T](using m: Mirror.Of[T], naming: Convention): Configurable[T] =
        inline m match
            case given Mirror.SumOf[T] => deriveSum[T]
            case given Mirror.ProductOf[T] =>
                val defaults = Default[T].values
                deriveProduct[T](defaults)

    private inline def summonOrdinal[T <: Tuple](ord: Int, idx: Int)(using
        naming: Convention
    ): Option[Configurable[?]] =
        inline erasedValue[T] match
            case _: EmptyTuple => None
            case _: (t *: ts) =>
                if ord == idx then Option(summonConfigurable[t])
                else summonOrdinal[ts](ord, idx + 1)

    private inline def summonConfigurable[T](using naming: Convention) = summonFrom:
        case cfg: Configurable[T] => cfg
        case given Mirror.Of[T]   => Configurable.derived[T]
        case _                    => summonInline[Configurable[T]]

    class ConfigurableFn[T](f: (Config, String) => T) extends Configurable[T]:
        def get(config: Config, key: String): T = f(config, key)

    private inline def deriveSum[T](using mirror: Mirror.SumOf[T], naming: Convention): Configurable[T] = 
        def getConfigurable(config: Config, key: String): T =
            config.getValue(key).valueType() match
                case ConfigValueType.OBJECT => readTypeFamily[T](config, key)
                case ConfigValueType.STRING => readEnum[T](config, key)
                case _ =>
                    throw new ConfigException.WrongType(
                        config.origin(),
                        s"STRING or OBJECT with `${naming.descriminator}` field expected"
                    )
        new ConfigurableFn[T](getConfigurable)
    end deriveSum

    class VariantsPF(subtypeLabel: String) extends PartialFunction[(Set[String], Int), Int] {
      override def apply(x: (Set[String], Int)): Int = x._2

      override def isDefinedAt(x: (Set[String], Int)): Boolean = x._1.contains(subtypeLabel)
    }

    private inline def readTypeFamily[T](
        config: Config,
        key: String
    )(using
        mirror: Mirror.SumOf[T],
        naming: Convention
    ): T =
        val conf         = Conf(config.underlying.getConfig(key), config.keys :+ key)
        val subtypeLabel = conf.getString(naming.descriminator)
        val labels       = getLabels[mirror.MirroredElemLabels]
        labels
            .zipWithIndex.map { (l, i) => naming.variants(l).toSet -> i }
            .collectFirst(new VariantsPF(subtypeLabel))
            .flatMap {
                case idx if idx >= 0 && idx < labels.size =>
                    summonOrdinal[mirror.MirroredElemTypes](idx, 0)
                case _ => None
            }
            .getOrElse(throw new ConfigException.BadValue(
              config.origin(),
              key,
              s"no subtype found for name: $subtypeLabel"
            ))
            .asInstanceOf[Configurable[T]]
            .get(config, key)

    private inline def readEnum[T](
        config: Config,
        key: String
    )(using
        mirror: Mirror.SumOf[T],
        naming: Convention
    ): T =
        val enumLabel = config.getString(key)
        val labels    = summonLabels[mirror.MirroredElemLabels]
        labels
            .zipWithIndex.map { (l, i) => naming.variants(l).toSet -> i }
            .collectFirst(new VariantsPF(enumLabel))
            .flatMap {
                case idx if idx >= 0 && idx < labels.size =>
                    summonCase[mirror.MirroredElemTypes, T](idx, 0)
                case _ => None
            }
            .getOrElse(
              throw new ConfigException.BadValue(
                config.origin(),
                config.completePath(key),
                s"no enum found for name: $enumLabel"
              )
            )

    private inline def summonCase[Types <: Tuple, T](ord: Int, idx: Int): Option[T] =
        inline erasedValue[Types] match
            case _: (head *: tail) =>
                inline summonInline[Mirror.Of[head]] match
                    case m: Mirror.Singleton =>
                        m.fromProduct(EmptyTuple) match
                            case t: T =>
                                if ord == idx
                                then Some(t)
                                else summonCase[tail, T](ord, idx + 1)
                            case _ =>
                                summonCase[tail, T](ord, idx + 1)
                    case m => None
            case _: EmptyTuple => None

    private inline def summonLabels[Labels <: Tuple]: Vector[String] =
        inline erasedValue[Labels] match
            case _: (head *: tail) => constValue[head & String] +: summonLabels[tail]
            case _: EmptyTuple     => Vector.empty

    private inline def deriveProduct[T](defaults: Tuple)(using mirror: Mirror.ProductOf[T], naming: Convention): Configurable[T] =
        def getConfigurable(config: Config, key: String): T =
            val tuple = inline erasedValue[T] match
                case _: Tuple =>
                    val values = config.getList(key).zipWithIndex
                    readTupleFromList[mirror.MirroredElemTypes](config, key, values)
                case _ =>
                    val labels = getLabels[mirror.MirroredElemLabels]
                    readTuple[mirror.MirroredElemTypes](config.getConfig(key), labels, defaults)
            mirror.fromProduct(tuple)

        new ConfigurableFn(getConfigurable)
    end deriveProduct

    private inline def getLabels[Labels <: Tuple]: List[String] =
        inline erasedValue[Labels] match
            case _: (head *: tail) => constValue[head & String] :: getLabels[tail]
            case _: EmptyTuple     => Nil

    class RecoverPF(default: Any) extends PartialFunction[Throwable, Any] {
      override def apply(e: Throwable): Any =
        default match
            case Some(value) => value
            case None => throw e

      override def isDefinedAt(x: Throwable): Boolean = x.isInstanceOf[ConfigException]
    }

    private inline def readTuple[Types <: Tuple](
        config: Config,
        labels: List[String],
        defaults: Tuple
    )(using naming: Convention): Tuple =
        inline erasedValue[Types] match
            case _: (head *: tail) =>
                defaults match
                    case default *: defaultsTail =>
                        Try(summonConfigurable[head].getValue(config, labels.head))
                        .recover(new RecoverPF(default))
                        .get *: readTuple[tail](config, labels.tail, defaultsTail)
                    case _ => EmptyTuple
            case _: EmptyTuple => EmptyTuple

    private inline def readTupleFromList[Types <: Tuple](
        config: Config,
        key: String,
        values: List[(ConfigValue, Int)]
    )(using naming: Convention): Tuple = inline erasedValue[Types] match
        case _: (head *: tail) => values match
                case (v, i) :: rest =>
                    summonConfigurable[head].get(config.atPath(v, s"$key-$i"), s"$key-$i") *:
                        readTupleFromList[tail](config, key, rest)
                case Nil =>
                    throw new ConfigException.BadValue(config.origin(), config.completePath(key), "wrong list size")
        case _: EmptyTuple => EmptyTuple
end Configurable
