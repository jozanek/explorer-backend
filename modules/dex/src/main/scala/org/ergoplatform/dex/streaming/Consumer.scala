package org.ergoplatform.dex.streaming

import cats.Functor
import cats.effect.{Blocker, ConcurrentEffect, ContextShift, Timer}
import fs2.kafka.{AutoOffsetReset, ConsumerSettings, ConsumerStream}
import fs2.{Stream, kafka}
import org.ergoplatform.dex.settings.KafkaSettings
import org.ergoplatform.dex.streaming.kafkaSerialization._
import org.ergoplatform.dex.streaming.schemaCodecs._
import org.ergoplatform.explorer.db.models.Output

abstract class Consumer[F[_], S[_[_] <: F[_], _]] {

  /** Stream of outputs appearing in the blockchain.
    */
  def stream: S[F, Output]
}

object Consumer {

  def apply[F[_]: ConcurrentEffect: ContextShift: Timer](
    settings: KafkaSettings
  )(implicit blocker: Blocker): Consumer[F, Stream] = {
    val consumerStream = kafka.consumerStream[F]
    val consumerSettings =
      ConsumerSettings[F, String, Option[Output]]
        .withBlocker(blocker)
        .withAutoOffsetReset(AutoOffsetReset.Earliest)
        .withGroupId(settings.groupId)
        .withClientId(settings.clientId)
    new Live(consumerStream, consumerSettings, settings.topicId)
  }

  final private class Live[F[_]: ContextShift: Timer: Functor](
    consumerStream: ConsumerStream[F],
    settings: ConsumerSettings[F, String, Option[Output]],
    topicId: String
  ) extends Consumer[F, Stream] {

    def stream: Stream[F, Output] =
      consumerStream
        .using(settings)
        .evalTap(_.subscribeTo(topicId))
        .flatMap(_.stream.map(_.record.value).unNone)
  }
}
