lazy val akkaV            = "2.5.17"
lazy val akkaHTTPV        = "10.1.5"
lazy val doobieV          = "0.6.0"
lazy val logbackV         = "1.2.3"
lazy val circeSupportV    = "1.22.0"
lazy val circeV           = "0.10.1"
lazy val akkaStreamKafka  = "0.22"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization  := "fr.xebia",
      scalaVersion  := "2.12.7",
      version       := "0.1.0-SNAPSHOT",
      scalacOptions := Seq("-Ypartial-unification", "-unchecked", "-feature", "-deprecation", "-encoding", "utf8")
    )),
    name := "event_sourcing_et_rgpd",
    shellPrompt := { state =>
      s"\033[38;5;45m[${name.value}] \033[38;5;21m> \033[0m"
    },

    libraryDependencies ++=  Seq(
      "com.typesafe.akka"     %% "akka-slf4j"                        % akkaV,
      "com.typesafe.akka"     %% "akka-stream"                       % akkaV,
      "com.typesafe.akka"     %% "akka-http"                         % akkaHTTPV,
      "com.typesafe.akka"     %% "akka-http-core"                    % akkaHTTPV,
      "com.typesafe.akka"     %% "akka-stream-kafka"                 % akkaStreamKafka,
      "de.heikoseeberger"     %% "akka-http-circe"                   % circeSupportV,
      "io.circe"              %% "circe-core"                        % circeV,
      "io.circe"              %% "circe-generic"                     % circeV,
      "io.circe"              %% "circe-parser"                      % circeV,
      "org.tpolecat"          %% "doobie-core"                       % doobieV,
      "org.tpolecat"          %% "doobie-postgres"                   % doobieV,
      "ch.qos.logback"        %  "logback-classic"                   % logbackV,
    ),
    packageName in Docker := "xebicon18-es-rgpd",
    version in Docker := "es",
    dockerExposedPorts := Seq(9000)
  )
  .enablePlugins(JavaAppPackaging)
