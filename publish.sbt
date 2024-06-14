publishTo              := sonatypePublishToBundle.value
sonatypeProfileName    := "io.github.ancane"
sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository     := "https://s01.oss.sonatype.org/service/local"

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/ancane/recon4s"),
    "scm:git@github.com:ancane/recon4s.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id = "ancane",
    name = "Ihor Shymko",
    email = "igor.shimko@gmail.com",
    url = url("https://github.com/ancane")
  )
)

ThisBuild / description := "Reloadable configs for Scala 3"
ThisBuild / licenses := List(
  "MIT" -> new URL("https://github.com/ancane/recon4s/blob/main/LICENSE")
)
ThisBuild / homepage             := Some(url("https://github.com/ancane/recon4s"))
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := {
    val nexus = "https://s01.oss.sonatype.org/"
    if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true

//sonatypeLogLevel := "DEBUG"
