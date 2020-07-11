maintainer in Linux := "AmirHossein Bahrami <a.bahrami9675@gmail.com>"
name := """ergo-proxy"""
organization := "ergo"

/*
enablePlugins(GitVersioning)

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

git.gitUncommittedChanges in ThisBuild := true
*/

version := "2.2.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala, PlayEbean)

scalaVersion := "2.12.10"

val javaVersion = settingKey[String]("The version of Java used for building.")

javaVersion := System.getProperty("java.version")

val java9AndSupLibraryDependencies: Seq[sbt.ModuleID] =
  if (!javaVersion.toString.startsWith("1.8")) {
    Seq(
      "com.sun.activation" % "javax.activation" % "1.2.0",
      "com.sun.xml.bind" % "jaxb-core" % "2.3.0",
      "com.sun.xml.bind" % "jaxb-impl" % "2.3.1",
      "javax.jws" % "javax.jws-api" % "1.1",
      "javax.xml.bind" % "jaxb-api" % "2.3.0",
      "javax.xml.ws" % "jaxws-api" % "2.3.1"
    )
  } else {
    Seq.empty
  }

libraryDependencies += guice

libraryDependencies ++= Seq(
  "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % Test,
  "org.scalaj" %% "scalaj-http" % "2.4.2",
  "org.eclipse.jetty" % "jetty-servlet" % "9.4.24.v20191120",
  "org.eclipse.jetty" % "jetty-server" % "9.4.24.v20191120",
  "io.swagger.parser.v3" % "swagger-parser-v3" % "2.0.18",
  "com.github.alanverbner" %% "bip39" % "0.1",
  "org.xerial" % "sqlite-jdbc" % "3.30.1",
  "com.payintech" %% "play-ebean" % "19.10",
  "io.ebean" % "ebean-test" % "12.1.10",
  "io.ebean.test" % "ebean-test-config" % "11.41.2",
  "org.mockito" % "mockito-core" % "3.3.0"
) ++ java9AndSupLibraryDependencies

// Circe dependency
val circeVersion = "0.12.3"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

// Appkit dependencies
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
  case path if path.contains("ebean") => MergeStrategy.first
  case PathList("META-INF", _ @ _*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}

// Assembly build plugin
mainClass in assembly := Some("play.core.server.ProdServerStart")
fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value)

assemblyMergeStrategy in assembly := {
  case PathList("reference.conf") => MergeStrategy.concat

  case manifest if manifest.contains("MANIFEST.MF") =>
    // We don't need the manifest files since sbt-assembly will create one with the given settings
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

coverageExcludedPackages := "<empty>;.*ProxyConfig.*;.*MiningAction.*;.*Logger.*;.*Helper.*;" +
  ".*MiningDisabledException.*;.*route.*;.*Route.*;.*EagerLoaderModule.*;.*StartupService.*;" +
  ".*LowerLayerNodeInterface.*;.*ProxyStatus.*;.*Status.*;.*Encryption.*;" +
  ".*NodeClient.*;.*Pool.*;"

coverageMinimum := 85
coverageFailOnMinimum := true
