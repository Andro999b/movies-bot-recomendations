name := "movies-bot-recomendations"

version := "0.1"

scalaVersion := "2.12.12"

val akkaVersion = "2.6.8"
val akkaHttpVersion = "10.2.1"
val sparkVersion = "3.0.1"

// spark
libraryDependencies ++= {
  Seq(
    "org.apache.spark" %% "spark-core" % sparkVersion,
    "org.apache.spark" %% "spark-mllib" % sparkVersion,
    "org.apache.spark" %% "spark-mllib" % sparkVersion
  )
}

// spark aws
libraryDependencies ++= {
  Seq(
    "com.audienceproject" %% "spark-dynamodb" % "1.1.0",
    "com.amazonaws" % "aws-java-sdk-s3" % "1.11.563"
  )
}

// akka
libraryDependencies ++= {
  Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion
  )
}




