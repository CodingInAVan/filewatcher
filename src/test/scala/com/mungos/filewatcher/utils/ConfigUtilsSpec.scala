package com.mungos.filewatcher.utils

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.mungos.filewatcher.{ConsoleDestination, FileConfig, HttpDestination, KafkaDestination, WatcherConfig}
import org.scalatest.funsuite.AsyncFunSuite
import org.scalatest.matchers.should.Matchers
import com.mungos.filewatcher.utils.configUtils.*

import scala.concurrent.duration.*

class ConfigUtilsSpec extends AsyncFunSuite with Matchers with AsyncIOSpec {

	test("Config reader should correctly decode a valid WatcherConfig JSON") {
		val configJson =
			"""
				|{
				|  "fileConfigs": [
				|    {
				|      "file": "file1.txt",
				|      "interval": "1s",
				|      "destination": {
				|        "type": "http",
				|        "uri": "https://example.com/api"
				|      }
				|    },
				|    {
				|      "file": "file2.txt",
				|      "interval": "2s",
				|      "destination": {
				|        "type": "kafka",
				|        "brokers": "localhost:9092",
				|        "topic": "my-topic"
				|      }
				|    },
				|    {
				|      "file": "file3.txt",
				|      "interval": "1s",
				|      "destination": {
				|        "type": "console"
				|      }
				|    }
				|  ]
				|}
				|""".stripMargin

		val expectedResult = WatcherConfig(
			List(
				FileConfig("file1.txt", 1.seconds, HttpDestination("https://example.com/api")),
				FileConfig("file2.txt", 2.seconds, KafkaDestination("localhost:9092", "my-topic")),
				FileConfig("file3.txt", 1.seconds, ConsoleDestination)
			)
		)

		val actualResult: IO[WatcherConfig] = decodeWatcherConfig(configJson)

		actualResult.asserting(_ shouldEqual expectedResult)
	}
}