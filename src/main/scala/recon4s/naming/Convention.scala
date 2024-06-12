package recon4s.naming

final case class Convention(
    from: Naming,
    to: Vector[Naming],
    substitutes: Map[String, String] = Map.empty,
    descriminator: String = "type"
):

    final def variants(key: String): Vector[String] =
        substitutes.get(key) match
            case Some(variant) => Vector(variant)
            case None          => 
              val parsed = from.parse(key)
              (to.map(_.format(parsed)) :+ key).distinct

    final def substitute(classField: String, configKey: String): Convention =
        copy(substitutes = substitutes.updated(classField, configKey))

    final def withDescriminaton(descr: String): Convention = copy(descriminator = descr)

object Convention:

    val CamelToCebab = Convention(
      from = CamelCase,
      to = Vector(CebabCase)
    )

    val CamelToCebabCamel = Convention(
      from = CamelCase,
      to = Vector(CebabCase, CamelCase)
    )

    val CamelToCebabCamelCaps = Convention(
      from = CamelCase,
      to = Vector(CebabCase, CamelCase, CamelCaps)
    )

    val CamelToCebabCamelCapsSnake = Convention(
      from = CamelCase,
      to = Vector(CebabCase, CamelCase, CamelCaps, SnakeCase)
    )
