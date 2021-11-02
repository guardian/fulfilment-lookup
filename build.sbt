name := "fulfilment-lookup"

organization := "com.gu"

description:= "Validates fulfilment information for a given subscription name"

version := "1.0"

scalaVersion := "2.12.4"

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
riffRaffArtifactResources += (file("cloudformation.yaml"), "cfn/cfn.yaml")

val jacksonVersion = "2.13.0"

libraryDependencies ++= Seq(
  "commons-logging" % "commons-logging" % "1.2",
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.0",
  "com.amazonaws" % "aws-lambda-java-log4j" % "1.0.1",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.12.99",
  "com.github.melrief" %% "purecsv" % "0.1.1",
  "com.squareup.okhttp3" % "okhttp" % "3.10.0",
  "com.typesafe.play" %% "play-json" % "2.9.2",
  "org.apache.logging.log4j" % "log4j-core" % "2.11.0",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test",
  "org.scalaz" % "scalaz-core_2.12" % "7.2.21",
  "org.mockito" % "mockito-core" % "2.18.0" % "test",
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonVersion,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion,
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % jacksonVersion
)

initialize := {
  val _ = initialize.value
  assert(sys.props("java.specification.version") == "1.8",
    "Java 8 is required for this project.")
}

//Having a merge strategy here is necessary as there is an conflict in the file contents for the jackson libs, there are two same versions with different contents.
//As a result we're picking the first file found on the classpath - this may not be required if the contents match in a future release
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case x => MergeStrategy.first
} 
