// SpatialOS Nexus access
val nexus = Map(
  "snapshots" -> ("snapshots" at "https://releases.service.improbable.io/content/repositories/snapshots/"),
  "releases" -> ("releases" at "https://releases.service.improbable.io/content/repositories/releases/")
)

resolvers.in(Global) ++= nexus.values.toSeq
credentials.in(Global) ++= (Path.userHome / ".ivy2" / "improbable.credentials").get.map(Credentials(_))
