package scala.meta.internal

import scala.collection.mutable
import scala.tools.nsc.{Global => NscGlobal, SubComponent}
import scala.tools.nsc.interpreter.{ReplGlobal => NscReplGlobal}
import scala.tools.nsc.interactive.{
  Global => NscInteractiveGlobal,
  InteractiveAnalyzer => NscInteractiveAnalyzer
}
import scala.tools.nsc.doc.ScaladocGlobal
import scala.tools.nsc.typechecker.SemanticdbAnalyzer

trait HijackAnalyzer extends SemanticdbAnalyzer { self: SemanticdbPlugin =>

  def hijackAnalyzer(): global.analyzer.type = {
    // Do nothing if running under ScaladocGlobal, see
    // https://github.com/scalameta/scalameta/issues/1072
    if (global.isInstanceOf[ScaladocGlobal]) return global.analyzer
    // NOTE: need to hijack the right `analyzer` field - it's different for batch compilers and repl compilers
    val isRepl = global.isInstanceOf[NscReplGlobal]
    val isInteractive = global.isInstanceOf[NscInteractiveGlobal]
    val newAnalyzer = {
      if (isInteractive) {
        new {
          val global: self.global.type with NscInteractiveGlobal =
            self.global.asInstanceOf[self.global.type with NscInteractiveGlobal]
        } with SemanticdbAnalyzer with NscInteractiveAnalyzer {
          override def newNamer(context: Context) =
            new SemanticdbNamer(context) with InteractiveNamer
          override def newTyper(context: Context) =
            new SemanticdbTyper(context) with InteractiveTyper
        }
      } else {
        new { val global: self.global.type = self.global } with SemanticdbAnalyzer {
          override protected def findMacroClassLoader(): ClassLoader = {
            val loader = super.findMacroClassLoader
            if (isRepl) {
              macroLogVerbose(
                "macro classloader: initializing from a REPL classloader: %s".format(
                  global.classPath.asURLs))
              val virtualDirectory = global.settings.outputDirs.getSingleOutput.get
              new scala.reflect.internal.util.AbstractFileClassLoader(virtualDirectory, loader) {}
            } else {
              loader
            }
          }
        }
      }
    }
    val globalClass: Class[_] =
      if (isRepl) global.getClass
      else if (isInteractive) classOf[NscInteractiveGlobal]
      else classOf[NscGlobal]
    val analyzerField = globalClass.getDeclaredField("analyzer")
    analyzerField.setAccessible(true)
    analyzerField.set(global, newAnalyzer)

    val phasesSetMapGetter = classOf[NscGlobal].getDeclaredMethod("phasesSet")
    val phasesSet = phasesSetMapGetter.invoke(global).asInstanceOf[mutable.Set[SubComponent]]
    if (phasesSet.exists(_.phaseName == "typer")) { // `scalac -help` doesn't instantiate standard phases
      def subcomponentNamed(name: String) = phasesSet.find(_.phaseName == name).head
      val oldScs @ List(oldNamer, oldPackageobjects, oldTyper) = List(
        subcomponentNamed("namer"),
        subcomponentNamed("packageobjects"),
        subcomponentNamed("typer"))
      val newScs =
        List(newAnalyzer.namerFactory, newAnalyzer.packageObjects, newAnalyzer.typerFactory)
      def hijackDescription(pt: SubComponent, sc: SubComponent) = {
        val phasesDescMapGetter = classOf[NscGlobal].getDeclaredMethod("phasesDescMap")
        val phasesDescMap =
          phasesDescMapGetter.invoke(global).asInstanceOf[mutable.Map[SubComponent, String]]
        phasesDescMap(sc) = phasesDescMap(pt)
      }
      oldScs zip newScs foreach { case (pt, sc) => hijackDescription(pt, sc) }
      phasesSet --= oldScs
      phasesSet ++= newScs
    }

    newAnalyzer.asInstanceOf[global.analyzer.type]
  }
}
