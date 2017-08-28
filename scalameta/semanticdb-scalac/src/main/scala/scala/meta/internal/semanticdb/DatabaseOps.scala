package scala.meta.internal
package semanticdb

import scala.tools.nsc.Global

trait DatabaseOps
    extends ConfigOps
    with DenotationOps
    with DocumentOps
    with InputOps
    with LanguageOps
    with MessageOps
    with ParseOps
    with PrinterOps
    with ReporterOps
    with ReflectionToolkit
    with SymbolOps {
  val global: Global
}
