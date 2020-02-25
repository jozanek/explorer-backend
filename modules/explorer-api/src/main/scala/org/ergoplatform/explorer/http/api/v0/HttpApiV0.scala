package org.ergoplatform.explorer.http.api.v0

import cats.effect.{ConcurrentEffect, ContextShift, Resource, Timer}
import cats.{~>, Monad}
import cats.syntax.semigroupk._
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.explorer.CRaise
import org.ergoplatform.explorer.Err.{RefinementFailed, RequestProcessingErr}
import org.ergoplatform.explorer.db.algebra.LiftConnectionIO
import org.ergoplatform.explorer.http.api.settings.HttpSettings
import org.ergoplatform.explorer.http.api.v0.routes.{
  AddressesRoutes,
  AssetsRoutes,
  BlocksRoutes,
  DexRoutes,
  DocsRoutes,
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
  ](settings: HttpSettings, protocolSettings: ProtocolSettings)(xa: D ~> F)(
    implicit e: ErgoAddressEncoder
  ): Resource[F, Server[F]] =
    for {
      blockChainService <- Resource.liftF(BlockChainService(xa))
      routes = BlocksRoutes(blockChainService) <+>
      AssetsRoutes(AssetsService(xa)) <+>
      DexRoutes(DexService(xa)) <+>
      TransactionsRoutes(TransactionsService(xa)) <+>
      AddressesRoutes(AddressesService(xa), TransactionsService(xa)) <+>
      StatsRoutes(StatsService(protocolSettings)(xa)) <+>
      DocsRoutes[F]
      http <- BlazeServerBuilder[F]
               .bindHttp(settings.port, settings.host)
               .withHttpApp(Router("/" -> routes).orNotFound)
               .resource
    } yield http
}
