package com.mungos.filewatcher

import cats.*
import cats.effect.*
import cats.effect.unsafe.implicits.global
import cats.implicits.*
import cats.syntax.all.*
import com.mungos.filewatcher.modules.{FileProcessor, FileReader, FileScheduler}
import com.mungos.filewatcher.utils.effectfulUtils.*
import fs2.Stream
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.io.File
import java.util.concurrent.Executors
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.*

object FileWatcherApp extends IOApp.Simple {
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  import com.mungos.filewatcher.utils.ConfigUtils._
  override def run: IO[Unit] = for
    raf <- Ref.of[IO, Map[String, Long]](Map.empty)
    json <- readConfigFile("src/main/resources/watcher-config.json")
    config <- decodeWatcherConfig(json)
    _ <- config.fileConfigs.parTraverse { fileConfig =>
      FileScheduler.make[IO](FileProcessor.make[IO]()).processFile(fileConfig, KeyValueRef[IO, String, Long](raf))
    }
  yield ()
}