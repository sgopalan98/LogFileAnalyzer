
name := "logFileAnalyzerService"

version := "0.1"

scalaVersion := "3.0.2"


javacOptions ++= Seq("-source", "1.6", "-target", "1.6", "-Xlint")




val typesafeConfigVersion = "1.4.1"
val scalacticVersion = "3.2.9"
val sfl4sVersion = "2.0.0-alpha5"
val AkkaVersion = "2.6.17"
val AkkaHttpVersion = "10.2.6"

lazy val root = (project in file(".")).
  settings(
    name := "logFileAnalyzerService",
    version := "1.0",
    scalaVersion := "3.0.2",
    retrieveManaged := true,
    libraryDependencies += "com.amazonaws" % "aws-lambda-java-core" % "1.2.1",
    libraryDependencies += "com.amazonaws" % "aws-lambda-java-events" % "3.10.0",
    // https://mvnrepository.com/artifact/com.amazonaws/aws-java-sdk-s3
    libraryDependencies += "com.amazonaws" % "aws-java-sdk-s3" % "1.12.99",
    libraryDependencies += "org.scalatest" %% "scalatest" % scalacticVersion % Test,
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
    ),
    libraryDependencies ++= Seq(
      "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion
    ),
    libraryDependencies ++= Seq(
      ("com.typesafe.akka" %% "akka-actor-typed" % "2.6.16").cross(CrossVersion.for3Use2_13),
      ("com.typesafe.akka" %% "akka-http"        % "10.2.6").cross(CrossVersion.for3Use2_13),
      ("com.typesafe.akka" %% "akka-stream"      % "2.6.16").cross(CrossVersion.for3Use2_13),
    ),
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x => MergeStrategy.first
    }
  )

