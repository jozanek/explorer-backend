package org.ergoplatform.explorer.http.api.v0.defs

import org.ergoplatform.explorer.Err.ApiErr
import org.ergoplatform.explorer.Id
import org.ergoplatform.explorer.http.api.v0.models.BlockSummary
import sttp.tapir._

object BlocksEndpointDefs {

  private val PathPrefix = "blocks"

  def blockSummaryById: Endpoint[Id, ApiErr, BlockSummary, Nothing] =
    baseEndpointDef
      .in(PathPrefix)
      .in(path[Id])
      .out(jsonBody[BlockSummary])
}