import mill._
import mill.scalalib._


object wmcwf extends SbtModule {
    def scalaVersion = "2.12.8"
    def ivyDeps = Agg(ivy"org.scala-lang.modules::scala-java8-compat:0.9.0")

    def unmanagedClasspath = T {
        if(!ammonite.ops.exists(millSourcePath / "lib")) Agg()
        else Agg.from(ammonite.ops.ls(millSourcePath / "lib").map(PathRef(_)))
    }
    object test extends Tests {
        def ivyDeps = Agg(ivy"org.scalatest::scalatest:3.0.5")
        def testFrameworks = Seq("org.scalatest.tools.Framework")
    }
}
