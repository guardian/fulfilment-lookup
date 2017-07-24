name := "fulfilment-lookup"

organization := "com.gu"

description:= "Validates fulfilment information for a given subscription name"

version := "1.0"

scalaVersion := "2.12.1"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-target:jvm-1.8",
  "-Ywarn-dead-code"
)

lazy val root = (project in file(".")).enablePlugins(RiffRaffArtifact)

assemblyJarName := s"fulfilment-lookup.jar"
riffRaffPackageType := assembly.value
riffRaffUploadArtifactBucket := Option("riffraff-artifact")
riffRaffUploadManifestBucket := Option("riffraff-builds")
riffRaffManifestProjectName := "MemSub::Membership Admin::Fulfilment Lookup"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-core" % "1.1.0",
  "com.amazonaws" % "aws-lambda-java-log4j" % "1.0.0",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.163",
  "log4j" % "log4j" % "1.2.17",
  "com.github.melrief" %% "purecsv" % "0.0.9",
  "com.typesafe.play" %% "play-json" % "2.6.2",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.mockito" % "mockito-core" % "1.9.5" % "test"
)

initialize := {
  val _ = initialize.value
  assert(sys.props("java.specification.version") == "1.8",
    "Java 8 is required for this project.")
}
