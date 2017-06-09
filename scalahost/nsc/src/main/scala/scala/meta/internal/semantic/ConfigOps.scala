package scala.meta.internal
package semantic

import scala.meta.internal.io.PathIO
import scala.meta.internal.scalahost.ScalahostPlugin
import scala.meta.io._

case class ScalahostConfig(sourceroot: AbsolutePath, semanticdb: SemanticdbMode) {
  def syntax: String =
    s"-P:${ScalahostPlugin.name}:sourceroot:$sourceroot " +
      s"-P:${ScalahostPlugin.name}:semanticdb:$semanticdb"
}
object ScalahostConfig {
  def default = ScalahostConfig(PathIO.workingDirectory, SemanticdbMode.Fat)
}

sealed abstract class SemanticdbMode {
  import SemanticdbMode._
  def isSlim: Boolean = this == Slim
  def isFat: Boolean = this == Fat
  def isDisabled: Boolean = this == Disabled
}
object SemanticdbMode {
  def unapply(arg: String): Option[SemanticdbMode] =
    all.find(_.toString.equalsIgnoreCase(arg))
  def all = List(Fat, Slim, Disabled)
  case object Fat extends SemanticdbMode
  case object Slim extends SemanticdbMode
  case object Disabled extends SemanticdbMode
}

trait ConfigOps { self: DatabaseOps =>
  val SetSemanticdb = "semanticdb:(.*)".r
  val SetSourceroot = "sourceroot:(.*)".r

  var config: ScalahostConfig = ScalahostConfig.default
  implicit class XtensionScalahostConfig(ignored: ScalahostConfig) {
    def setSourceroot(sourceroot: AbsolutePath): Unit =
      config = config.copy(sourceroot = sourceroot)
    def setSemanticdbMode(mode: SemanticdbMode): Unit =
      config = config.copy(semanticdb = mode)
  }
}
