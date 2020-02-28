package org.ergoplatform.explorer.http.api.v0

import cats.effect.{ConcurrentEffect, ContextShift, Resource, Timer}
import cats.Monad
import cats.syntax.semigroupk._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.CRaise
import org.ergoplatform.explorer.Err.{RefinementFailed, RequestProcessingErr}
import org.ergoplatform.explorer.db.Trans
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.http.api.settings.HttpSettings
import org.ergoplatform.explorer.http.api.v0.routes.{
  AddressesRoutes,
  AssetsRoutes,
  BlocksRoutes,
  DexRoutes,
  DocsRoutes,
  InfoRoutes,
  StatsRoutes,
  TransactionsRoutes
}
import org.ergoplatform.explorer.http.api.v0.services.{
  AddressesService,
  AssetsService,
  BlockChainService,
  DexService,
  StatsService,
  TransactionsService
}
import org.ergoplatform.explorer.settings.ProtocolSettings
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.{Router, Server}
import org.http4s.syntax.kleisli._

object HttpApiV0 {

  /** Create an API v0 http server.
    */
  def apply[
    F[_]: ConcurrentEffect: ContextShift: Timer,
    D[_]: CRaise[*[_], RequestProcessingErr]: CRaise[*[_], RefinementFailed]: Monad: LiftConnectionIO
  ](settings: HttpSettings, protocolSettings: ProtocolSettings)(trans: D Trans F)(
    implicit e: ErgoAddressEncoder
  ): Resource[F, Server[F]] =
    for {
      blockChainService <- Resource.liftF(BlockChainService(trans))
      blockRoutes       <- Resource.liftF(BlocksRoutes(blockChainService))
      assetRoutes       <- Resource.liftF(AssetsRoutes(AssetsService(trans)))
      dexRoutes         <- Resource.liftF(DexRoutes(DexService(trans)))
      txRoutes          <- Resource.liftF(TransactionsRoutes(TransactionsService(trans)))
      addressRoutes <- Resource.liftF(
                        AddressesRoutes(
                          AddressesService(trans),
                          TransactionsService(trans)
                        )
                      )
      statsService = StatsService(protocolSettings)(trans)
      infoRoutes  <- Resource.liftF(InfoRoutes(statsService))
      statsRoutes <- Resource.liftF(StatsRoutes(statsService))
      docsRoutes  <- Resource.liftF(DocsRoutes[F])

      routes = infoRoutes <+> blockRoutes <+> assetRoutes <+> dexRoutes <+> txRoutes <+>
               addressRoutes <+> statsRoutes <+> docsRoutes
      http <- BlazeServerBuilder[F]
               .bindHttp(settings.port, settings.host)
               .withHttpApp(Router("/" -> routes).orNotFound)
               .resource
    } yield http
}
