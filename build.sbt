import org.scalajs.linker.interface.ESVersion
import org.scalajs.linker.interface.OutputPatterns
import org.scalajs.linker.interface.ModuleSplitStyle
import org.openqa.selenium.firefox.{FirefoxOptions, FirefoxDriverLogLevel}
import org.scalajs.jsenv.selenium.SeleniumJSEnv

lazy val publishSettings = Seq(
  organization := "dev.bluepitaya",
  organizationName := "blue.pitaya",
  organizationHomepage := Some(url("https://bluepitaya.dev")),
  scmInfo :=
    Some(
      ScmInfo(
        url("https://github.com/blue-pitaya/laminar-contenteditable"),
        "scm:git@github.com:blue-pitaya/laminar-contenteditable.git"
      )
    ),
  developers :=
    List(
      Developer(
        id = "blue.pitaya",
        name = "blue.pitaya",
        email = "blue.pitaya@pm.me",
        url = url("https://bluepitaya.dev")
      )
    ),
  licenses := List(License.MIT),
  homepage := Some(url("https://bluepitaya.dev")),
  description :=
    "Library for simulating extended textarea using contenteditable.",
  // Remove all additional repository other than Maven Central from POM
  pomIncludeRepository := { _ =>
    false
  },
  publishMavenStyle := true,
  publishTo := {
    val nexus = "https://s01.oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else Some("releases" at nexus + "service/local/staging/deploy/maven2")
  }
)

lazy val baseSettings = Seq(scalaVersion := "2.13.8", version := "0.2")

lazy val root = (project in file("."))
  .settings(baseSettings)
  .settings(publishSettings)
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name := "laminar-contenteditable",
    scalacOptions := Seq("-Xlint"),
    libraryDependencies += "com.raquo" %%% "laminar" % "16.0.0",
    libraryDependencies += "org.scalatest" %%% "scalatest" % "3.2.15" % Test
    // UNCOMMENT THIS TO ENABLE TESTS
    // jsEnv := new SeleniumJSEnv(new FirefoxOptions().setLogLevel(FirefoxDriverLogLevel.FATAL), SeleniumJSEnv.Config().withKeepAlive(false)),
  )

lazy val example = (project in file("example"))
  .dependsOn(root)
  .settings(baseSettings)
  .settings(
    scalacOptions := Seq("-Xlint"),
    name := "laminar-contenteditable-example",
    libraryDependencies += "org.planet42" %%% "laika-core" % "0.19.0",
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.ESModule)
        .withOutputPatterns(OutputPatterns.fromJSFile("%s.js"))
        .withESFeatures(_.withESVersion(ESVersion.ES2021))
    },
    scalaJSUseMainModuleInitializer := true,
    Compile / fastLinkJS / scalaJSLinkerOutputDirectory :=
      baseDirectory.value / "ui/sccode/",
    Compile / fullLinkJS / scalaJSLinkerOutputDirectory :=
      baseDirectory.value / "ui/sccode/"
  )
  .enablePlugins(ScalaJSPlugin)
