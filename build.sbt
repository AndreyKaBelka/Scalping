ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.6.4"

lazy val common = project.settings(
  name := "Common project",
  libraryDependencies ++= commonDeps,
)

lazy val prediction = (project in file("PredictionProject"))
  .dependsOn(common)
  .settings(
    name := "Prediction project",
  )

lazy val squeezer = (project in file("SqueezeFounder"))
  .dependsOn(common)
  .settings(
    name := "Squeeze founder project",
  )

lazy val dataProvider = (project in file("DataProvider"))
  .dependsOn(common)
  .settings(
    name := "Data Provider",
  )

lazy val root = (project in file("."))
  .aggregate(dataProvider, prediction, squeezer)
  .settings(
    name         := "Scalping",
    scalaVersion := "3.6.4",
  )

val commonDeps = Seq(
  "dev.zio"              %% "zio-streams"        % "2.1.16",
  "dev.zio"              %% "zio"                % "2.1.16",
  "dev.zio"              %% "zio-http"           % "3.0.1",
  "dev.zio"              %% "zio-logging"        % "2.3.2",
  "dev.zio"              %% "zio-interop-cats"   % "23.1.0.3",
  "dev.zio"              %% "zio-logging-slf4j2" % "2.5.0",
  "ch.qos.logback"        % "logback-classic"    % "1.5.11",
  "org.ta4j"              % "ta4j-core"          % "0.17",
  "dev.zio"              %% "zio-json"           % "0.7.39",
  "io.dropwizard.metrics" % "metrics-core"       % "4.2.28",
  "dev.zio"              %% "zio-redis"          % "1.1.2",
//  "dev.zio"              %% "zio-schema"         % "1.6.5",
  "dev.zio"              %% "zio-schema-json"    % "1.6.1",
  "dev.zio"              %% "zio-parser"         % "0.1.11",
)

scalacOptions += "-Ymacro-annotations"
