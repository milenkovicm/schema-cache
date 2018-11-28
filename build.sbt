import sbt.Keys.libraryDependencies
// *****************************************************************************
// Projects
// *****************************************************************************

lazy val `schema-cache` =
  project
    .in(file("."))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(settings)
    .settings(
      libraryDependencies ++= Seq(
        library.scalaTest % Test,
        library.scalaCache,
        library.scalaCacheCaffeine,
        library.slf4jSimple,
        library.jsonValidator,
        library.jacksonScala,
        //library.scalaLogging
      ),
      libraryDependencies ++= library.akka,
      libraryDependencies ++= library.akkaHttp
    )

// *****************************************************************************
// Library dependencies
// *****************************************************************************

lazy val library =
  new {

    object Version {
      val scalaTest     = "3.0.5"
      val scalaCache    = "0.26.0"
      val slf4j         = "1.7.25"
      val akka          = "2.5.18"
      val akkaHttp      = "10.1.5"
      val jsonValidator = "2.2.10"
      val jacksonScala  = "2.9.7"
    }

    val scalaTest          = "org.scalatest"                %% "scalatest"            % Version.scalaTest
    val scalaCacheCaffeine = "com.github.cb372"             %% "scalacache-caffeine"  % Version.scalaCache % Test
    val scalaCache         = "com.github.cb372"             %% "scalacache-caffeine"  % Version.scalaCache
    val slf4jSimple        = "org.slf4j"                    % "slf4j-simple"          % Version.slf4j
    val jacksonScala       = "com.fasterxml.jackson.module" %% "jackson-module-scala" % Version.jacksonScala % Test
    val jsonValidator      = "com.github.java-json-tools"   % "json-schema-validator" % Version.jsonValidator % Test

    val akkaHttp = Seq(
      "com.typesafe.akka" %% "akka-http-core"    % Version.akkaHttp,
      "com.typesafe.akka" %% "akka-http"         % Version.akkaHttp,
      "com.typesafe.akka" %% "akka-http-testkit" % Version.akkaHttp % Test
    )

    val akka = Seq(
      "com.typesafe.akka" %% "akka-actor"          % Version.akka,
      "com.typesafe.akka" %% "akka-stream"         % Version.akka,
      "com.typesafe.akka" %% "akka-stream-testkit" % Version.akka
    )

  }

// *****************************************************************************
// Settings
// *****************************************************************************

lazy val settings =
commonSettings ++
scalafmtSettings

lazy val commonSettings =
  Seq(
    // // scalaVersion from .travis.yml via sbt-travisci
    //scalaVersion := "2.12.7",
    organization := "com.github.milenkovicm.schema.cache",
    organizationName := "Marko Milenkovic",
    startYear := Some(2018),
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0")),
    scalacOptions ++= Seq(
      "-unchecked",
      "-deprecation",
      "-language:_",
      "-target:jvm-1.8",
      "-encoding",
      "UTF-8",
      "-Ypartial-unification",
      "-Ywarn-unused-import"
    ),
    Compile / unmanagedSourceDirectories := Seq((Compile / scalaSource).value),
    Test / unmanagedSourceDirectories := Seq((Test / scalaSource).value),
    testFrameworks += new TestFramework("utest.runner.Framework")
  )

lazy val scalafmtSettings =
  Seq(
    scalafmtOnCompile := true
  )
