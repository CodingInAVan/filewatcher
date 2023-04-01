package com.mungos.filewatcher.modules

import cats.effect.kernel.Sync
import cats.effect.{IO, Resource}
import cats.syntax.all.*

import java.io.{BufferedReader, File, InputStreamReader}
import java.util.stream.Collectors
import scala.jdk.CollectionConverters.*

trait FileReader[F[_]]:
  def skip(n: Long): F[Unit]
  def length: F[Long]
  def lines: F[List[String]]

object FileReader {
  def make[F[_]: Sync](file: File) = Resource.make {
    Sync[F].delay(scala.io.Source.fromFile(file).bufferedReader()).handleErrorWith { throwable =>
      Sync[F].raiseError(new RuntimeException(s"Failed to load file: ${throwable.getMessage}"))
    }
  } { reader =>
    Sync[F].delay(reader.close())
  }.map( reader =>
    new FileReader[F]:
      override def skip(n: Long): F[Unit] = Sync[F].delay(reader.skip(n))

      override def length: F[Long] = Sync[F].delay(file.length())

      override def lines: F[List[String]] = Sync[F].delay(reader.lines().collect(Collectors.toList).asScala.toList)
  )
}
