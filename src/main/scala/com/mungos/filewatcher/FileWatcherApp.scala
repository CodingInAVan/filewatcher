package com.mungos.filewatcher

import cats.*
import cats.effect.*
import cats.effect.unsafe.implicits.global
import cats.implicits.*
import cats.syntax.all.*
import com.mungos.filewatcher.modules.{FileProcessor, FileReader, FileScheduler, HttpClientPool, KafkaMessageSenderFactory, KafkaMessageSenderPool}
import com.mungos.filewatcher.utils.effectfulUtils.{KeyValueRef, *}
import fs2.Stream
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.io.File
import java.util.concurrent.Executors
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.*

object FileWatcherApp extends IOApp {
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  import com.mungos.filewatcher.utils.configUtils._
  override def run(args: List[String]): IO[ExitCode] = {
    val configFilename = args.headOption.getOrElse("src/main/resources/watcher-config.json")
    val fixedPoolSize = 10

    val kafkaMessageSenderFactory = KafkaMessageSenderFactory.make[IO]
    val fileProcessor = FileProcessor.make[IO]()

    for
      raf <- Ref.of[IO, Map[String, Long]](Map.empty)
      json <- readConfigFile(configFilename)
      config <- decodeWatcherConfig(json)
      kafkaMessageSenderPool <- KafkaMessageSenderPool.make[IO](fixedPoolSize, kafkaMessageSenderFactory)
      httpPostClientPool <- HttpClientPool.make[IO](fixedPoolSize)
      _ <- config.fileConfigs.parTraverse { fileConfig => {
          FileScheduler.make[IO](fileProcessor).processFile(fileConfig, KeyValueRef[IO, String, Long](raf), httpPostClientPool, kafkaMessageSenderPool)
        }
      }
    yield ExitCode.Success
  }
}