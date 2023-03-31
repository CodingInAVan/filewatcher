package com.mungos.filewatcher

import cats.*
import cats.effect.*
import cats.effect.unsafe.implicits.global
import cats.implicits.*
import cats.syntax.all.*
import com.mungos.filewatcher.io.FileReader
import com.mungos.filewatcher.modules.{FileProcessor, FileScheduler}
import com.mungos.filewatcher.utils.effectfulUtils.*
import fs2.Stream
import fs2.concurrent.SignallingRef
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.io.File
import java.util.concurrent.Executors
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.*

object FileWatcherApp extends IOApp.Simple {

  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val config: WatcherConfig = WatcherConfig(
    List(
      FileConfig(file = "src\\main\\scala\\com\\mungos\\filewatcher\\WatcherConfig.scala", interval = 1.second),
      FileConfig(file = "src\\main\\scala\\com\\mungos\\filewatcher\\FileWatcherApp.scala", interval = 2.second),
    ))

  override def run: IO[Unit] = for
    raf <- Ref.of[IO, Map[String, Long]](Map.empty)
    _ <- config.fileConfigs.parTraverse { fileConfig =>
      FileScheduler.make[IO](FileProcessor.make[IO]()).processFile(fileConfig, KeyValueRef[IO, String, Long](raf))
    }
  yield ()
}