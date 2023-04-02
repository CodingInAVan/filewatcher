package com.mungos.filewatcher.modules

import cats.effect._
import cats.syntax.all._
import cats.effect.testing.scalatest.AsyncIOSpec
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.concurrent.duration._

class KafkaMessageSenderPoolSpec extends AsyncFunSuite with Matchers with AsyncIOSpec {

	given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
	// Mock KafkaMessageSenderFactory
	val mockKafkaMessageSenderFactory: KafkaMessageSenderFactory[IO] = new KafkaMessageSenderFactory[IO] {
		override def create(brokers: String): KafkaMessageSender[IO] =
			new KafkaMessageSender[IO] {
				override def send(topic: String, key: String, value: String): IO[Unit] = IO.unit
			}
	}

	test("KafkaMessageSenderPool should correctly create, reuse, and release KafkaMessageSenders") {
		val poolSize = 2
		val broker = "localhost:9092"

		for {
			pool <- KafkaMessageSenderPool.make[IO](poolSize, mockKafkaMessageSenderFactory)
			sender1 <- pool.get(broker)
			_ <- pool.release(broker, sender1)
			sender2 <- pool.get(broker)
			_ <- pool.release(broker, sender2)
			sender3 <- pool.get(broker)
			sender4 <- pool.get(broker)
			_ <- pool.release(broker, sender3)
			_ <- pool.release(broker, sender4)
			poolSizeAfterTest <- pool.size
		} yield {
			sender1 shouldEqual sender2
			sender3 shouldEqual sender4
			poolSizeAfterTest shouldEqual 1
		}
	}
}
