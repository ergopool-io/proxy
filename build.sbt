maintainer in Linux := "AmirHossein Bahrami <a.bahrami9675@gmail.com>"
name := """ergo-proxy"""
organization := "ergo"

version := "0.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.1"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test
libraryDependencies += "org.scalaj" %% "scalaj-http" % "2.4.2"

val circeVersion = "0.12.3"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

libraryDependencies ++= Seq(
  "org.eclipse.jetty" % "jetty-servlet" % "9.4.24.v20191120",
  "org.eclipse.jetty" % "jetty-server" % "9.4.24.v20191120"
)

libraryDependencies += "io.swagger.parser.v3" % "swagger-parser-v3" % "2.0.16"

// Assembly build plugin
mainClass in assembly := Some("play.core.server.ProdServerStart")
fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value)

assemblyMergeStrategy in assembly := {
  case manifest if manifest.contains("MANIFEST.MF") =>
    // We don't need manifest files since sbt-assembly will create
    // one with the given settings
    MergeStrategy.discard
  case referenceOverrides if referenceOverrides.contains("reference-overrides.conf") =>
    // Keep the content for all reference-overrides.conf files
    MergeStrategy.concat
  case x =>
    // For all the other files, use the default sbt-assembly merge strategy
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

javaOptions in Test += "-Dconfig.file=conf/test.conf"