import sbt._

class ScalaHttpProject(info: ProjectInfo) 
  extends DefaultProject(info) {

  val jettyVersion = "7.2.2.v20101205"
  val jettyHttp = "org.eclipse.jetty" % "jetty-client" % jettyVersion % "provided"

  val specs = "org.scala-tools.testing" % "specs_2.8.1" % "1.6.7" % "test"
}
