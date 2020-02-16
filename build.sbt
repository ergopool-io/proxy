maintainer in Linux := "AmirHossein Bahrami <a.bahrami9675@gmail.com>"
name := """ergo-proxy"""
organization := "ergo"

/*enablePlugins(GitVersioning)

version in ThisBuild := {
  if (git.gitCurrentTags.value.nonEmpty) {
    git.gitDescribedVersion.value.get
  } else {
    if (git.gitHeadCommit.value.contains(git.gitCurrentBranch.value)) {
        git.gitHeadCommit.value.get.take(8) + "-SNAPSHOT"
    } else {
      git.gitCurrentBranch.value + "-" + git.gitHeadCommit.value.get.take(8) + "-SNAPSHOT"
    }
  }
}

git.gitUncommittedChanges in ThisBuild := true*/

version := "0.4"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.10"

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

libraryDependencies += "io.swagger.parser.v3" % "swagger-parser-v3" % "2.0.8"

libraryDependencies += "com.github.alanverbner" %% "bip39" % "0.1"

lazy val sonatypePublic = "Sonatype Public" at "https://oss.sonatype.org/content/groups/public/"
lazy val sonatypeReleases = "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
lazy val sonatypeSnapshots = "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

resolvers ++= Seq(sonatypeReleases,
  "SonaType" at "https://oss.sonatype.org/content/groups/public",
  "Typesafe maven releases" at "http://repo.typesafe.com/typesafe/maven-releases/",
  sonatypeSnapshots,
  Resolver.mavenCentral)

libraryDependencies += "org.ergoplatform" % "ergo-wallet_2.12" % "3.2.0"
libraryDependencies += "org.ergoplatform" % "ergo-appkit_2.12" % "develop-60478389-SNAPSHOT"

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

// Assembly build plugin
mainClass in assembly := Some("play.core.server.ProdServerStart")
fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value)

assemblyMergeStrategy in assembly := {
  case PathList("reference.conf") => MergeStrategy.concat

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

coverageExcludedPackages := "<empty>;.*List.*;.*MiningAction.*;.*Logger.*;.*Helper.*;.*MiningDisabledException.*;.*NotEnoughBoxesException.*;.*PoolRequestException.*;.*route.*;.*Route.*;.*EagerLoaderModule.*;.*StartupService.*"
coverageMinimum := 85
coverageFailOnMinimum := true