package scala.meta
package io

import java.io._
import java.nio.{file => nio}
import java.net._
import org.scalameta.data._
import scala.meta.internal.io.{FileIO, PathIO}

@data class AbsolutePath private (value: String) {
  def syntax: String = value
  def structure: String = s"""AbsolutePath("$value")"""
  override def toString: String = syntax

  def toFile: File = new File(value)
  def toURI: URI = new URI(s"file:$value")
  def toNIO: nio.Path = nio.Paths.get(toURI)
  @deprecated("Use toString() instead", "1.8")
  def absolute: String = toString()

  def toRelative: RelativePath = toRelative(PathIO.workingDirectory)
  def toRelative(path: AbsolutePath): RelativePath = PathIO.unresolve(path, this)
  def toRelative(file: File): RelativePath = toRelative(AbsolutePath(file))
  def toRelative(path: String): RelativePath = toRelative(AbsolutePath(path))

  def resolve(path: RelativePath): AbsolutePath = PathIO.resolve(this, path)
  def resolve(file: File): AbsolutePath = resolve(RelativePath(file))
  def resolve(path: String): AbsolutePath = resolve(RelativePath(path))

  def isFile: Boolean = FileIO.isFile(this)
  def isDirectory: Boolean = FileIO.isDirectory(this)
  def readAllBytes: Array[Byte] = FileIO.readAllBytes(this)
}

object AbsolutePath {
  def apply(path: nio.Path): AbsolutePath = {
    AbsolutePath(path.toString)
  }

  def apply(file: File): AbsolutePath = {
    AbsolutePath(file.getAbsolutePath)
  }

  def apply(path: String): AbsolutePath = {
    if (PathIO.isAbsolutePath(path)) new AbsolutePath(path)
    else sys.error(s"not an absolute path: $path")
  }
}
