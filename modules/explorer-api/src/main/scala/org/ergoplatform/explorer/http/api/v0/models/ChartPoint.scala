package org.ergoplatform.explorer.http.api.v0.models

import io.circe.Codec
import io.circe.derivation.deriveCodec
import org.ergoplatform.explorer.db.models.aggregates.TimePoint
import sttp.tapir.{Schema, Validator}

final case class ChartPoint(timestamp: Long, value: Long)

object ChartPoint {

  def apply(point: TimePoint[Long]): ChartPoint =
    ChartPoint(point.ts, point.value)

  implicit val codec: Codec[ChartPoint] = deriveCodec

  implicit val schema: Schema[ChartPoint] = Schema.derived

  implicit val validator: Validator[ChartPoint] = schema.validator
}
