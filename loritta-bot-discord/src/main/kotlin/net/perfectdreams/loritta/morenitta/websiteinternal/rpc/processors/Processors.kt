package net.perfectdreams.loritta.morenitta.websiteinternal.rpc.processors

import net.perfectdreams.loritta.morenitta.websiteinternal.InternalWebServer
import net.perfectdreams.loritta.morenitta.websiteinternal.rpc.processors.loritta.DailyShopRefreshedProcessor
import net.perfectdreams.loritta.morenitta.websiteinternal.rpc.processors.loritta.GetLorittaInfoProcessor

class Processors(val internalWebServer: InternalWebServer) {
    val getLorittaInfoProcessor = GetLorittaInfoProcessor(internalWebServer.m)
    val executeDashGuildScopedProcessor = ExecuteDashGuildScopedProcessor(internalWebServer, internalWebServer.m)
    val dailyShopRefreshedProcessor = DailyShopRefreshedProcessor(internalWebServer.m)
}