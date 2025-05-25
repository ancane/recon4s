# recon4s [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.ancane/recon4s_3/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.ancane/recon4s_3)
> Reloadable configs for Scala 3

## Motivation
Imagine a legacy config that follows no strict naming rules, having a mixture of kebab, camel and snake case keys.

Being able to put mixed-case config into a case class was the primary goal.

Secondly, if the whole config or part of it is fetched from a file or network, it would be great to refresh it once in a while and not restart the whole app.

## Main features
- recon4s can read mixed-case configs
- supports hot-reload

## Details
- Defines `Configurable[T]` type class. Instances for commonly used Scala and Java types provided
- Supports enums, trait families, default values
- `Reloadable[T]` trait marks hot-reloadable members
- Adds no result wrappers. Throws exceptions (com.typesafe.config.ConfigException)
- Assumes case class fields follow camelCase naming and looks for kebab-case, camelCase, CamelCaps and snake_case config keys by default. Convention is configurable via given override

## Sbt

`
libraryDependencies += "io.github.ancane" %% "recon4s" % "0.4"
`

## Import
`import recon4s.{*, given}`

## Basic example

`com.typesafe.config.Config` extention methods `.as[T]` and `.as[T]("path")` read and return an instance of type T.

```scala
import com.typesafe.config.*
import recon4s.{*, given}

case class AppConf(
    appName: String,
    appVersion: String = "0.1",
    snakeBites: Boolean
) derives Configurable

val config = ConfigFactory.parseString(
    """|
       |    appName = recon4s
       |    app-version = "1.2.3"
       |    snake_bites = yes
       |
       |""".stripMargin
)

val appConfig: AppConf = config.as[AppConf]

```

## Reloadable example

`watchConfig[T]` and `watchConfigPath[T]("path")` functions read `freshConfig` return instance of type `T` and update each `Reloadable[T]` encountered in `T` every `reloadInterval` and report success or failure to a corresponding callback.

```scala
import com.typesafe.config.*
import recon4s.{*, given}
import scala.concurrent.duration.*

case class AppConf(
    appName: String,
    featureFlag: Reloadable[Boolean]
) derives Configurable

val appConfig = watchConfig[AppConf](
    freshConfig = ConfigFactory.parseString("{ include file(...) }"),
    reloadInterval = 1.minute,
    onReloadFailure = (e => println(s"reload failure: ${e.getMessage()}")),
    onReloadSuccess = (path => println(s"$path reload success"))
)

// When `featureFlag` changes inside the included file,
// featureFlag.get() returns updated value
def flag = appConfig.featureFlag.get()

```

## Naming


Conventions are:

- CamelToCebab
- CamelToCebabCamel
- CamelToCebabCamelCaps
- CamelToCebabCamelCapsSnake (default)

`CamelToCebabCamelCapsSnake` means, that given `fieldName`, recon4s will look for config key named `field-name` or `fieldName` or `FieldName` or `field_name` or `as-is` in that order.

Switching to stricter convention:

```scala
given Convention = recon4s.CamelToCebab
```

Config keys direct override:
```scala
given Convention = recon4s.CamelToCebab.substitute("one", "TWO")

case class One(one: String)

ConfigFactory
    .parseString("{ TWO = 1}")
    .as[One]
```

## Custom Configurable[T] instances

```scala
given (using c: Configurable[String]): Configurable[LocalDate] =
    c.map(LocalDate.parse(_, DateTimeFormatter.ofPattern("yyyy/MM/dd")))

case class LocalDateConf(date: LocalDate)

val actual = ConfigFactory
    .parseString("date = 2007/12/03")
    .as[LocalDateConf]

```

## trait/enum type information field override
`type` key is used by default to get the type information when reading trait families or enums (with parameters).

Changing default type key:

```scala
given Convention = recon4s.CamelToCebab.withDescriminator("name")
```
