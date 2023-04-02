package com.mungos.filewatcher.modules

import cats.effect.*
import cats.syntax.all.*
import cats.effect.std.Queue
import com.mungos.filewatcher.utils.effectfulUtils.KeyValueRef
import org.typelevel.log4cats.Logger

/**
 * KafkaMessageSenderPool is a trait representing a pool of KafkaMessageSender instances.
 * This pool is used to manage and reuse KafkaMessageSender instances based on the brokers string.
 */
trait KafkaMessageSenderPool[F[_]]:
	/**
	 * Get a KafkaMessageSender instance for the given brokers.
	 * If a KafkaMessageSender instance for the brokers already exists, it will be reused.
	 * Otherwise, a new instance will be created and added to the pool.
	 *
	 * @param brokers The brokers string used to connect to the Kafka cluster.
	 * @return A KafkaMessageSender instance for the specified brokers.
	 */
	def get(brokers: String): F[KafkaMessageSender[F]]
	def release(brokers: String, sender: KafkaMessageSender[F]): F[Unit]
	def size: F[Int]
object KafkaMessageSenderPool {
	def make[F[_] : Async : Logger](
		poolSize: Int,
		kafkaMessageSenderFactory: KafkaMessageSenderFactory[F]
	): F[KafkaMessageSenderPool[F]] =
		for {
			pool <- Ref.of[F, Map[String, Queue[F, KafkaMessageSender[F]]]](Map.empty)
			keyValueRef = KeyValueRef[F, String, Queue[F, KafkaMessageSender[F]]](pool)
		} yield new KafkaMessageSenderPool[F]:
			override def get(brokers: String): F[KafkaMessageSender[F]] =
				keyValueRef.get(brokers).flatMap {
					case Some(queue) =>
						Logger[F].info(s"Reusing KafkaMessageSender for brokers: $brokers") *>
							queue.take
					case None =>
						for {
							_ <- Logger[F].info(s"Creating KafkaMessageSender queue for brokers: $brokers")
							newQueue <- Queue.bounded[F, KafkaMessageSender[F]](poolSize)
							_ <- keyValueRef.put(brokers, newQueue)
							sender = kafkaMessageSenderFactory.create(brokers)
							_ <- newQueue.offer(sender)
						} yield sender
				}

			override def release(brokers: String, sender: KafkaMessageSender[F]): F[Unit] =
				keyValueRef.get(brokers).flatMap {
					case Some(queue) =>
						Logger[F].info(s"Releasing KafkaMessageSender for brokers: $brokers") *>
							queue.offer(sender)
					case None =>
						Logger[F].error(s"Attempting to release a KafkaMessageSender for unknown brokers: $brokers")
				}

			override def size: F[Int] = keyValueRef.ref.get.map(_.size)
}
