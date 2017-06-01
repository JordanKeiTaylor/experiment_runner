// SpatialOS JVM artifact packaging for the gsim
packAutoSettings

packJarNameConvention := "full"

packExcludeJars := Seq(
  "scala-compiler-.*\\.jar",
  "arch-util_2.11.*\\.jar",
  "fabric-core_2.11.*\\.jar",
  "deployment_2.11.*\\.jar" // Sadly these are applied before the full names are set.
)

pack := {
  val packDir = pack.value
  val libDir = packDir / "lib"
  val outputDir = (baseDirectory.value / SpatialOSBuild.outputBaseDir / "gsim").getCanonicalFile
  if (outputDir.exists()) {
    streams.value.log.info(s"Cleaning output directory $outputDir")
    IO.delete(IO.listFiles(outputDir))
  } else {
    IO.createDirectory(outputDir)
  }
  streams.value.log.info(s"Copying $libDir to $outputDir")
  IO.listFiles(libDir).foreach { f =>
    val destFile = new File(outputDir, f.getName().replaceAll("[^a-zA-Z0-9_@.]", "_"))
    IO.copyFile(f, destFile);
  }
  outputDir
}

// `workerPackage` is the common interface called by the build scripts
lazy val workerPackage = TaskKey[Any]("workerPackage")

workerPackage := {
  pack.value
}