package net.perfectdreams.loritta.morenitta.website.routes.dashboard.configure

import io.ktor.server.application.*
import net.dv8tion.jda.api.entities.Guild
import net.perfectdreams.i18nhelper.core.I18nContext
import net.perfectdreams.loritta.common.locale.BaseLocale
import net.perfectdreams.loritta.common.utils.UserPremiumPlans
import net.perfectdreams.loritta.morenitta.LorittaBot
import net.perfectdreams.loritta.morenitta.dao.ServerConfig
import net.perfectdreams.loritta.morenitta.website.evaluate
import net.perfectdreams.loritta.morenitta.website.routes.dashboard.RequiresGuildAuthLocalizedDashboardRoute
import net.perfectdreams.loritta.morenitta.website.utils.WebsiteUtils
import net.perfectdreams.loritta.morenitta.website.utils.extensions.legacyVariables
import net.perfectdreams.loritta.morenitta.website.utils.extensions.respondHtml
import net.perfectdreams.loritta.morenitta.website.views.dashboard.guild.LegacyPebbleGuildDashboardRawHtmlView
import net.perfectdreams.loritta.morenitta.website.views.dashboard.guild.general.GuildGeneralView
import net.perfectdreams.loritta.serializable.ColorTheme
import net.perfectdreams.loritta.serializable.config.GuildGeneralConfigBootstrap
import net.perfectdreams.loritta.temmiewebsession.LorittaJsonWebSession
import net.perfectdreams.temmiediscordauth.TemmieDiscordAuth

class ConfigureGeneralRoute(loritta: LorittaBot) : RequiresGuildAuthLocalizedDashboardRoute(loritta, "/configure") {
	override suspend fun onDashboardGuildAuthenticatedRequest(call: ApplicationCall, locale: BaseLocale, i18nContext: I18nContext, discordAuth: TemmieDiscordAuth, userIdentification: LorittaJsonWebSession.UserIdentification, guild: Guild, serverConfig: ServerConfig, colorTheme: ColorTheme) {	val variables = call.legacyVariables(loritta, locale)
		if (true) {
			variables["saveType"] = "default"

			call.respondHtml(
				LegacyPebbleGuildDashboardRawHtmlView(
					loritta,
					i18nContext,
					locale,
					getPathWithoutLocale(call),
					loritta.getLegacyLocaleById(locale.id),
					userIdentification,
					UserPremiumPlans.getPlanFromValue(loritta.getActiveMoneyFromDonations(userIdentification.id.toLong())),
					colorTheme,
					guild,
					"Painel de Controle",
					evaluate("configure_server.html", variables),
					"default"
				).generateHtml()
			)
		} else {
			val serializableGuild = WebsiteUtils.convertJDAGuildToSerializable(guild)
			val serializableUser = WebsiteUtils.convertUserIdentificationToSerializable(userIdentification)
			val serializableSelfLorittaUser = WebsiteUtils.convertJDAUserToSerializable(guild.selfMember.user)

			val bootstrap = GuildGeneralConfigBootstrap(
				serializableGuild,
				serializableUser,
				serializableSelfLorittaUser,
				GuildGeneralConfigBootstrap.GuildGeneralConfig(
					serverConfig.commandPrefix,
					serverConfig.deleteMessageAfterCommand,
					serverConfig.warnOnUnknownCommand,
					serverConfig.blacklistedChannels,
					serverConfig.warnIfBlacklisted,
					serverConfig.blacklistedWarning
				)
			)

			call.respondHtml(
				GuildGeneralView(
					loritta.newWebsite!!,
					i18nContext,
					locale,
					getPathWithoutLocale(call),
					loritta.getLegacyLocaleById(locale.id),
					userIdentification,
					UserPremiumPlans.getPlanFromValue(loritta.getActiveMoneyFromDonations(userIdentification.id.toLong())),
					colorTheme,
					guild,
					"general",
					bootstrap
				).generateHtml()
			)
		}
	}
}