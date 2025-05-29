package recon4s.naming

final case class Convention(
    to: Naming,
    from: Vector[Naming],
    substitutes: Map[String, String] = Map.empty,
    descriminator: String = "type"
):

    final def variants(key: String): Vector[String] =
        substitutes.get(key) match
            case Some(variant) => Vector(variant)
            case None          => 
              val parsed = to.parse(key)
              (from.map(_.format(parsed)) :+ key).distinct

    final def substitute(classField: String, configKey: String): Convention =
        copy(substitutes = substitutes.updated(classField, configKey))

    final def withDescriminaton(descr: String): Convention = copy(descriminator = descr)

object Convention:

    val CamelFromDash = Convention(
      to = CamelCase,
      from = Vector(DashCase)
    )

    val CamelFromDashCamel = Convention(
      to = CamelCase,
      from = Vector(DashCase, CamelCase)
    )

    val CamelFromDashCamelCaps = Convention(
      to = CamelCase,
      from = Vector(DashCase, CamelCase, CamelCaps)
    )

    val CamelFromDashCamelCapsSnake = Convention(
      to = CamelCase,
      from = Vector(DashCase, CamelCase, CamelCaps, SnakeCase)
    )
