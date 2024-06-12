package recon4s.deriving

import scala.quoted.*
import scala.deriving.*
import scala.compiletime.constValue

private[recon4s] sealed trait Default[T]:
    type Out <: Tuple
    def values: Out

object Default:
    def apply[T](using d: Default[T]) = d

    transparent inline given [T](using m: Mirror.ProductOf[T]): Default[T] = new Default[T]:
        type Out = Tuple.Map[m.MirroredElemTypes, Option]
        def values =
            val tupleSize: Int = constValue[Tuple.Size[m.MirroredElemTypes]]
            getDefaults[T](tupleSize).asInstanceOf[Out]

    inline def getDefaults[T](inline s: Int): Tuple = ${ getDefaultsImpl[T]('s) }

    def getDefaultsImpl[T](s: Expr[Int])(using Quotes, Type[T]): Expr[Tuple] =
        import quotes.reflect.*

        val n = s.asTerm.underlying.asInstanceOf[Literal].constant.value.asInstanceOf[Int]

        val terms: List[Option[Term]] =
            (1 to n).toList.map(i =>
                TypeRepr.of[T].typeSymbol
                    .companionClass
                    .declaredMethod(s"$$lessinit$$greater$$default$$$i")
                    .headOption
                    .map(Select(Ref(TypeRepr.of[T].typeSymbol.companionModule), _))
            )

        def exprOfOption[T](oet: Option[Expr[T]])(using Type[T], Quotes): Expr[Option[T]] = oet match
            case None     => Expr(None)
            case Some(et) => '{ Some($et) }

        val exprs: List[Option[Expr[Any]]]  = terms.map(_.map(_.asExprOf[Any]))
        val exprs1: List[Expr[Option[Any]]] = exprs.map(exprOfOption)
        Expr.ofTupleFromSeq(exprs1)
