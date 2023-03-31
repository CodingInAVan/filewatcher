package com.mungos.filewatcher.utils

import cats.*
import cats.effect.*
import cats.implicits.*
import cats.syntax.all.*

object effectfulUtils:

  case class KeyValueRef[F[_] : Monad, K, V](ref: Ref[F, Map[K, V]]):
    def put(key: K, value: V): F[Unit] =
      ref.modify(map => (map.updated(key, value), ()))

    def get(key: K): F[Option[V]] =
      ref.get.map(_.get(key))

    def delete(key: K): F[Unit] =
      ref.modify(map => (map - key, ()))