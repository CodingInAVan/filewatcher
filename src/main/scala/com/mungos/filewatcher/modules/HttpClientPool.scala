package com.mungos.filewatcher.modules

import cats.effect.*
import cats.syntax.all.*
import cats.effect.std.{Queue, Semaphore}
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

trait HttpClientPool[F[_]]:
  def get: F[HttpPostClient[F]]
  def release(client: HttpPostClient[F]): F[Unit]
  def size: F[Int]

object HttpClientPool {
  def make[F[_]: Async](poolSize: Int): F[HttpClientPool[F]] =
    for {
      availableClients <- Queue.bounded[F, HttpPostClient[F]](poolSize)
      clients <- createHttpClient[F].replicateA(poolSize).use(_.pure[F])
      _ <- clients.traverse_(availableClients.offer)
    } yield new HttpClientPool[F]:
      override def get: F[HttpPostClient[F]] = availableClients.take

      override def release(client: HttpPostClient[F]): F[Unit] = availableClients.offer(client)

      override def size: F[Int] = availableClients.size

  private def createHttpClient[F[_]: Async]: Resource[F, HttpPostClient[F]] = EmberClientBuilder.default[F].build.map(HttpPostClient.make(_))
}
