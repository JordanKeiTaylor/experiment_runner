// This class provides common information about the SpatialOS application.

import sbt.Keys._
import sbt._
import Keys._
import scala.util.parsing.json.JSON

object SpatialOSBuild extends Build {
  // Json4s is not exported by default, but if it is imported, please use the following version, e.g.
  // libraryDependencies += "org.json4s" %% "json4s-native" % SpatialOSBuild.json4sVersion
  val json4sVersion = "3.2.11.1-Improbable"

  private val akkaVersion = "2.4.0"
  private val commonsIoVersion = "2.4"
  private val kryoVersion = "3.0.3"
  private val protobufVersion = "3.0.0-beta-2"
  private val sprayVersion = "1.3.2"

  private val projectManifestText = IO.read(file("../../spatialos.json")).trim
  private val projectManifestObj: Option[Any] = JSON.parseFull(projectManifestText)
  private val projectManifest: Map[String, Any] = projectManifestObj.get.asInstanceOf[Map[String, Any]]

  // The current version of the project, e.g. `0.1`
  val currentVersion = projectManifest.get("project_version").get.asInstanceOf[String]

  // The current version of the SpatialOS SDK that the project depends on, e.g. `5.0.0`
  val improbableVersion = scala.util.Properties.envOrNone("SPATIALOS_BUILD_NUMBER").getOrElse(projectManifest.get("sdk_version").get.asInstanceOf[String])

  // The name of the project
  val projectName = projectManifest.get("name").get.asInstanceOf[String]

  // A relative path to output artifacts to.
  val outputBaseDir = (file("..") / ".." / "build" / "assembly").toString

  // The root project object
  lazy val root = Project(id = projectName, base = file(".")).settings(
    scalaVersion := "2.11.7",
    version := currentVersion,
    unmanagedSourceDirectories.in(Compile) += baseDirectory.value / "generated",

    // APIs
    libraryDependencies += "improbable" %% "fabric-papi" % SpatialOSBuild.improbableVersion exclude("com.google.guava", "guava"),
    libraryDependencies += "improbable" %% "fabric-fapi" % SpatialOSBuild.improbableVersion exclude("com.google.guava", "guava"),
    libraryDependencies += "improbable" %% "fabric-dapi" % SpatialOSBuild.improbableVersion exclude("com.google.guava", "guava"),
    libraryDependencies += "improbable" %% "unity-sdk-fabric" % SpatialOSBuild.improbableVersion exclude("com.google.guava", "guava"),

    // Utils
    libraryDependencies += "improbable" %% "fabric-util" % SpatialOSBuild.improbableVersion exclude("com.google.guava", "guava"),

    // Protos
    libraryDependencies += "improbable" %% "api-protos" % SpatialOSBuild.improbableVersion exclude("com.google.guava", "guava"),
    libraryDependencies += "improbable" % "fabric-fapi-protocol" % SpatialOSBuild.improbableVersion,

    // Generated code dependencies
    libraryDependencies += "com.esotericsoftware" % "kryo" % kryoVersion intransitive(),
    libraryDependencies += "commons-io" % "commons-io" % commonsIoVersion intransitive(),

    publishMavenStyle := false,
    fork.in(runMain) := true,
    fork.in(Test) := true
  )
}
