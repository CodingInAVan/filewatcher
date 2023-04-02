package com.mungos.filewatcher.modules

import cats.*
import cats.effect.*
import cats.effect.unsafe.implicits.global
import cats.implicits.*
import cats.syntax.all.*
import com.mungos.filewatcher.utils.effectfulUtils.KeyValueRef
import com.mungos.filewatcher.{ConsoleDestination, FileConfig, FileWatcherApp, HttpDestination, KafkaDestination}
import fs2.Stream
import fs2.text.utf8
import org.typelevel.log4cats.Logger
import org.http4s.*

import java.io.File
import scala.concurrent.duration.FiniteDuration

trait FileProcessor[F[_]]:
  def getNewLinesFromFile(fileReader: FileReader[F], prevLength: Long): F[List[String]]
  def processingInterval(currentTime: FiniteDuration, fileConfig: FileConfig, filePositionMap: KeyValueRef[F, String, Long], httpClientPool: HttpClientPool[F], kafkaMessageSenderPool: KafkaMessageSenderPool[F]): F[Unit]

object FileProcessor {
  def make[F[_]: Sync: Logger](): FileProcessor[F] = new FileProcessor[F]:
    override def getNewLinesFromFile(fileReader: FileReader[F], prevLength: Long): F[List[String]] =
      for {
        _ <- fileReader.skip(prevLength)
        lines <- fileReader.lines
      } yield lines

    override def processingInterval(currentTime: FiniteDuration,
      fileConfig: FileConfig,
      filePositionMap: KeyValueRef[F, String, Long],
      httpClientPool: HttpClientPool[F],
      kafkaMessageSenderPool: KafkaMessageSenderPool[F]): F[Unit] =
      for {
        pos <- filePositionMap.get(fileConfig.file)
        file <- Sync[F].delay(new File(fileConfig.file))
        newLines <- FileReader.make[F](file).use { fileReader =>
          getNewLinesFromFile(fileReader, pos.getOrElse(0L))
        }
        _ <- fileConfig.destination match {
          case HttpDestination(uri) =>
            for {
              client <- httpClientPool.get
              _ <- Logger[F].info(s"Sending new lines from ${fileConfig.file} to ${uri}")
              _ <- client.post(Uri.unsafeFromString(uri), newLines.mkString(","))
              _ <- httpClientPool.release(client)
            } yield ()

          case KafkaDestination(brokers, topic) =>
            for {
              producer <- kafkaMessageSenderPool.get(brokers)
              _ <- producer.send(topic, fileConfig.file, newLines.mkString("\n"))
              _ <- kafkaMessageSenderPool.release(brokers, producer)
            } yield ()

          case ConsoleDestination =>
            Logger[F].info(newLines.mkString("\n"))
        }
        _ <- Logger[F].info(s"[$currentTime] ${newLines.mkString(",")}")
        _ <- filePositionMap.put(fileConfig.file, file.length())
      } yield ()
}