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
  "com.github.melrief" %% "purecsv" % "0.0.9",
  "com.squareup.okhttp3" % "okhttp" % "3.4.1",
  "com.typesafe.play" %% "play-json" % "2.6.2",
  "log4j" % "log4j" % "1.2.17",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  "org.scalaz" % "scalaz-core_2.12" % "7.2.14",
  "org.mockito" % "mockito-core" % "1.9.5" % "test",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.8.11.1"
)

initialize := {
  val _ = initialize.value
  assert(sys.props("java.specification.version") == "1.8",
    "Java 8 is required for this project.")
}
