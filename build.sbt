ThisBuild / organization         := "com.github.ancane"
ThisBuild / organizationHomepage := Some(url("https://github.com/ancane/recon4s"))
ThisBuild / version              := "0.4.3"

Global / onChangedBuildSource := ReloadOnSourceChanges

credentials += Credentials(Path.userHome / ".sbt" / "sonatype_central_credentials")

lazy val root = project
    .in(file("."))
    .settings(
      name         := "recon4s",
      scalaVersion := "3.4.1",
      scalacOptions ++= Seq(
        "-encoding",
        "utf8",
        "-deprecation",
        "-feature",
        "-unchecked",
//        "-Xmax-inlines", "100",
        "-language:experimental.macros",
        "-language:higherKinds",
        "-language:implicitConversions",
        "-Ykind-projector",
        "-Wvalue-discard",
        "-Wunused:implicits",
        "-Wunused:explicits",
        "-Wunused:imports",
        "-Wunused:locals",
        "-Wunused:params",
        "-Wunused:privates",
        "-Xfatal-warnings"
      ),
      // deps
      libraryDependencies += "com.typesafe"   % "config" % "1.4.3",
      libraryDependencies += "org.scalameta" %% "munit"  % "0.7.29" % Test
    )
