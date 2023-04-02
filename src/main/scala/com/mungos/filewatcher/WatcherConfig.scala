package com.mungos.filewatcher

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, HCursor, Json, DecodingFailure}
import io.circe.syntax.*
import io.circe.parser.*
import fs2.concurrent.Topic

import java.util.Properties
import scala.concurrent.duration.FiniteDuration

case class WatcherConfig (
  fileConfigs: List[FileConfig]
)
case class FileConfig (
  file: String,
  interval: FiniteDuration,
  destination: Destination
)

sealed trait Destination
case class HttpDestination(uri: String) extends Destination
case class KafkaDestination(brokers: String, topic: String) extends Destination
case object ConsoleDestination extends Destination

object Destination:
  given Decoder[Destination] with
    def apply(cursor: HCursor): Decoder.Result[Destination] =
      cursor.get[String]("type").flatMap {
        case "http" => cursor.as[HttpDestination]
        case "kafka" => cursor.as[KafkaDestination]
        case "console" => Right(ConsoleDestination)
        case _ => Left(DecodingFailure("Invalid Destination JSON", cursor.history))
      }

object HttpDestination:
  given Decoder[HttpDestination] = deriveDecoder
object KafkaDestination:
  given Decoder[KafkaDestination] = deriveDecoder
