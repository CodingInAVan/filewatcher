package com.mungos.filewatcher.utils

import cats.effect.IO
import com.mungos.filewatcher.WatcherConfig
import fs2.concurrent.SignallingRef
import fs2.io.file.{Files, Flag, Flags, Path}
import fs2.text.utf8
import io.circe.{Decoder, DecodingFailure}
import io.circe.generic.auto.*
import io.circe.parser.*

import scala.concurrent.duration.{Duration, FiniteDuration}

object ConfigUtils:
  def readConfigFile(file: String): IO[String] = {
    Files[IO]
      .readAll(Path(file), 4096, Flags.Read)
      .through(utf8.decode)
      .compile
      .string
  }

  def decodeWatcherConfig(json: String): IO[WatcherConfig] = {
    given Decoder[FiniteDuration] with
      def apply(c: io.circe.HCursor): Decoder.Result[FiniteDuration] = {
        c.as[String].flatMap { durationStr =>
          Duration(durationStr) match {
            case finiteDuration: FiniteDuration => Right(finiteDuration)
            case _ => Left(DecodingFailure("Invalid duration format", c.history))
          }
        }
      }

    decode[WatcherConfig](json) match {
      case Right(config) => IO.pure(config)
      case Left(error) => IO.raiseError(new RuntimeException(s"Error decoding JSON: $error"))
    }
  }
