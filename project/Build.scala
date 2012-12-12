import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "photobooks"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "com.twitter" % "finagle-core" % "5.3.22",
      "com.twitter" % "finagle-http" % "5.3.22",
      "commons-codec" % "commons-codec" % "1.7",
      "joda-time" % "joda-time" % "2.0",
      "com.mongodb.casbah" % "casbah_2.9.1" % "2.1.5-1"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      resolvers += "Twitter Finagle Resolver" at "http://maven.twttr.com",
      resolvers += "snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      resolvers += "releases"  at "https://oss.sonatype.org/content/groups/scala-tools"
    )

}
