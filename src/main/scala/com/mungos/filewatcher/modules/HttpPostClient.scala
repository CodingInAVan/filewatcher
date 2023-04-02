package com.mungos.filewatcher.modules

import cats.effect.*
import org.http4s.*
import org.http4s.Method.POST
import org.http4s.client.Client

trait HttpPostClient[F[_]]:
  def post(uri: Uri, body: String, headers: Headers = Headers.empty): F[String]

object HttpPostClient {
  def make[F[_]: Async](client: Client[F]): HttpPostClient[F] = new HttpPostClient[F]:
    override def post(uri: Uri, body: String, headers: Headers): F[String] =
      val request = Request[F](POST, uri, headers = headers).withEntity(body)
      client.expect[String](request)
}
