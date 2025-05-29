ThisBuild / publishTo := {
  val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
  if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
  else localStaging.value
}

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
ThisBuild / publishMavenStyle := true
