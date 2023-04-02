package com.mungos.filewatcher.modules

import cats.effect.Async
import org.apache.kafka.common.serialization.StringSerializer
import cats.syntax.functor.*
import org.typelevel.log4cats.Logger
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord, RecordMetadata}

import java.util.Properties
import java.util.concurrent.{CompletableFuture, Future}

trait KafkaMessageSender[F[_]]:
	def send(topic: String, key: String, value: String): F[Unit]
object KafkaMessageSender {
	def make[F[_]: Async](producer: KafkaProducer[String, String]): KafkaMessageSender[F] = new KafkaMessageSender[F]:
		override def send(topic: String, key: String, value: String): F[Unit] = {
			val record = new ProducerRecord[String, String](topic, key, value)
			val javaFuture: Future[RecordMetadata] = producer.send(record)
			val completableFuture: CompletableFuture[Void] = CompletableFuture.supplyAsync(() => javaFuture.get()).thenAccept(_ => ())
			Async[F].fromCompletableFuture(Async[F].delay(completableFuture)).void
		}
}

type Brokers = String

trait KafkaMessageSenderFactory[F[_]]:
	def create(brokers: String): KafkaMessageSender[F]

object KafkaMessageSenderFactory:
	def make[F[_] : Async : Logger]: KafkaMessageSenderFactory[F] = new KafkaMessageSenderFactory[F] {
		override def create(brokers: Brokers): KafkaMessageSender[F] = {
			val producerProps = new Properties()
			producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
			producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer])
			producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer])

			val producer = new KafkaProducer[String, String](producerProps)

			KafkaMessageSender.make[F](producer)
		}
	}

	def default[F[_] : Async : Logger]: KafkaMessageSenderFactory[F] = make[F]
