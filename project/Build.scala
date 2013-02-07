import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "photobooks"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "com.twitter" % "finagle-core" % "5.3.22",
      "com.twitter" % "finagle-http" % "5.3.22",
      "commons-codec" % "commons-codec" % "1.7",
      "joda-time" % "joda-time" % "2.0",
      "org.mongodb" %% "casbah" % "2.5.0",
      "org.scalatest" %% "scalatest" % "1.9.1" % "test"
    )

  val main = play.Project(appName, appVersion, appDependencies).settings(
      resolvers += "Twitter Finagle Resolver" at "http://maven.twttr.com",
      resolvers += "snapshots" at "https://oss.sonatype.org/content/repositories/snapshots",
      resolvers += "releases"  at "https://oss.sonatype.org/content/groups/scala-tools"
    )

}
