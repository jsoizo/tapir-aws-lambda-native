ThisBuild / scalaVersion := "2.13.6"
ThisBuild / organization := "com.jsoizo"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val tapirVersion = "0.19.0"
lazy val catsEffectVersion = "3.2.9"
lazy val circeVersion = "0.14.1"
lazy val logbackVersion = "1.2.3"
lazy val scalaLoggingVersion = "3.9.4"
lazy val http4sVersion = "0.23.6"
lazy val sttpVersion = "3.3.17"

lazy val api = project
  .in(file("api"))
  .settings(
    name := "api",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-core" % tapirVersion,
      "org.typelevel" %% "cats-effect" % catsEffectVersion
    )
  )
lazy val apiDependency: ClasspathDependency = api % "test->test;compile->compile"

lazy val local = project
  .in(file("local"))
  .settings(
    name := "local",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
      "org.http4s" %% "http4s-blaze-server" % http4sVersion
    )
  )
  .dependsOn(apiDependency)

lazy val lambdaNative = project
  .in(file("lambda-native"))
  .enablePlugins(NativeImagePlugin)
  .dependsOn(apiDependency)
  .settings(
    name := "lambda-native",
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %% "tapir-aws-lambda" % tapirVersion
    ) ++ Seq(
      "com.softwaremill.sttp.client3" %% "core",
      "com.softwaremill.sttp.client3" %% "circe",
      "com.softwaremill.sttp.client3" %% "httpclient-backend-fs2"
    ).map(_ % sttpVersion) ++ Seq(
      "io.circe" %% "circe-core",
      "io.circe" %% "circe-generic",
      "io.circe" %% "circe-parser"
    ).map(_ % circeVersion),
    Compile / mainClass := Some("com.jsoizo.LambdaApp"),
    nativeImageInstalled := true,
    nativeImageOptions += s"-H:ReflectionConfigurationFiles=${baseDirectory.value / "native-image-configs" / "reflect-config.json"}",
    nativeImageOptions += s"-H:ConfigurationFileDirectories=${baseDirectory.value / "native-image-configs"}",
    nativeImageOptions += "-H:+JNI",
    nativeImageOutput := target.value / "native-image" / "bootstrap",
    nativeImageVersion := "21.3"
  )
