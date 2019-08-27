val _scalaVersion = "2.12.4"

val akkaVersion = "2.5.11"
val akkaHttpVersion = "10.1.1"

resolvers in ThisBuild += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"
resolvers += Resolver.bintrayRepo("hseeberger", "maven")

val akkaDependencies = Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion
)

val httpDependencies = Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
  "com.typesafe.play" %% "play-json" % "2.6.9",
  "de.heikoseeberger" %% "akka-http-play-json" % "1.20.1"
)

val accountsRestApi = (Project("accounts-rest-api", file("."))
  settings(
    mainClass := Some("com.github.dedkovva.accounts.Boot"),
    scalaVersion := _scalaVersion,
    scalaVersion in ThisBuild := _scalaVersion,
    libraryDependencies ++= (akkaDependencies ++ httpDependencies ++ Seq(
      "org.scalatest" %% "scalatest" % "3.0.4" % "test",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "org.redisson" % "redisson" % "3.6.5",
      "com.github.kstyrc" % "embedded-redis" % "0.6",
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % "test"
    ))
  )
)