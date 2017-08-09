package scala.meta.internal

trait HijackReporter { self: SemanticdbPlugin =>

  def hijackReporter(): Unit = {
    g.reporter match {
      case _: SemanticdbReporter => // do nothing, already hijacked
      case underlying =>
        val semanticdbReporter = new SemanticdbReporter(underlying)
        g.reporter = semanticdbReporter
    }
  }
}
