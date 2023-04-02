package com.mungos.filewatcher.modules

import cats.effect.{Sync, Temporal}
import com.mungos.filewatcher.FileConfig
import com.mungos.filewatcher.utils.effectfulUtils.KeyValueRef
import fs2.Stream
import org.typelevel.log4cats.Logger

trait FileScheduler[F[_]]:
  def processFile(fileConfig: FileConfig, filePositionMap: KeyValueRef[F, String, Long], httpClientPool: HttpClientPool[F], kafkaMessageSenderPool: KafkaMessageSenderPool[F]): F[Unit]


object FileScheduler:
  def make[F[_]: Temporal : Sync : Logger](fileProcessor: FileProcessor[F]): FileScheduler[F] = new FileScheduler[F]:
    override def processFile(fileConfig: FileConfig, filePositionMap: KeyValueRef[F, String, Long], httpClientPool: HttpClientPool[F], kafkaMessageSenderPool: KafkaMessageSenderPool[F]): F[Unit] = Stream
      .awakeEvery[F](fileConfig.interval)
      .evalMap { currentTime =>
        fileProcessor.processingInterval(currentTime, fileConfig, filePositionMap, httpClientPool, kafkaMessageSenderPool)
      }.compile
      .drain