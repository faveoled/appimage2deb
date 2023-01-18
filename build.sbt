ThisBuild / scalaVersion := "3.2.1"

lazy val hello = (project in file("."))
  .enablePlugins(ScalaJSPlugin, ScalablyTypedConverterPlugin)
  .settings(
    name := "Scala.js Tutorial",
    scalaJSUseMainModuleInitializer := true,
    libraryDependencies += "org.rogach" %%% "scallop" % "4.1.0",
    libraryDependencies += "com.lihaoyi" %%% "utest" % "0.8.1" % "test",
    testFrameworks += new TestFramework("utest.runner.Framework"),
    Compile / npmDependencies ++= Seq(
      "@types/node" -> "18.11.18"
    ),
    stOutputPackage := "typings"
  )


// This is an application with a main method

//

// Add support for the DOM in `run` and `test`
//jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv()

// uTest settings
