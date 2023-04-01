package com.mungos.filewatcher.modules

import cats.*
import cats.effect.*
import cats.effect.unsafe.implicits.global
import cats.implicits.*
import cats.syntax.all.*
import com.mungos.filewatcher.utils.effectfulUtils.KeyValueRef
import com.mungos.filewatcher.{FileConfig, FileWatcherApp}
import fs2.Stream
import org.typelevel.log4cats.Logger

import java.io.File
import scala.concurrent.duration.FiniteDuration

trait FileProcessor[F[_]]:
  def getNewLinesFromFile(fileReader: FileReader[F], prevLength: Long): F[List[String]]
  def processingInterval(currentTime: FiniteDuration, fileConfig: FileConfig, filePositionMap: KeyValueRef[F, String, Long]): F[Unit]

object FileProcessor {
  def make[F[_]: Sync: Logger](): FileProcessor[F] = new FileProcessor[F]:
    override def getNewLinesFromFile(fileReader: FileReader[F], prevLength: Long): F[List[String]] =
      for {
        _ <- fileReader.skip(prevLength)
        lines <- fileReader.lines
      } yield lines

    override def processingInterval(currentTime: FiniteDuration, fileConfig: FileConfig, filePositionMap: KeyValueRef[F, String, Long]): F[Unit] = for {
      pos <- filePositionMap.get(fileConfig.file)
      file <- Sync[F].delay(new File(fileConfig.file))
      newLines <- FileReader.make[F](file).use { fileReader =>
        getNewLinesFromFile(fileReader, pos.getOrElse(0L))
      }
      _ <- Logger[F].info(s"[$currentTime] ${newLines.mkString(",")}")
      _ <- filePositionMap.put(fileConfig.file, file.length())
    } yield ()
}