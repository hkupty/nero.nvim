lazy val buildSettings = Seq(
  organization := "nero",
  version := "0.1.0",
  scalaVersion := "2.12.8",
  test in assembly := {},
  mainClass in assembly := Some("nero.Main")
)

assemblyJarName in assembly := "nero.jar"
assemblyMergeStrategy in assembly := {
   {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case x => MergeStrategy.first
   }
}

libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-nop" % "1.7.25",
  "com.ensarsarajcic.neovim.java" % "core-rpc" % "0.1.13"
)
