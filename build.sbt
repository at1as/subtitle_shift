scalaVersion := "2.12.3"

libraryDependencies ++= List(
  "org.typelevel" %% "cats-core" % "1.0.0",
  "org.scala-lang" % "scala-compiler" % scalaVersion.value)

scalacOptions ++= Seq(
  "-Xfatal-warnings",
  "-Ypartial-unification"
)

wartremoverErrors ++= Warts.unsafe

