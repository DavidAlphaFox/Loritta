package net.perfectdreams.loritta.morenitta.website.routes.dashboard.configure.twitch

import io.ktor.server.application.*
import net.dv8tion.jda.api.entities.Guild
import net.perfectdreams.i18nhelper.core.I18nContext
import net.perfectdreams.loritta.cinnamon.pudding.tables.DonationKeys
import net.perfectdreams.loritta.cinnamon.pudding.tables.servers.moduleconfigs.PremiumTrackTwitchAccounts
import net.perfectdreams.loritta.cinnamon.pudding.tables.servers.moduleconfigs.TrackedTwitchAccounts
import net.perfectdreams.loritta.common.locale.BaseLocale
import net.perfectdreams.loritta.common.utils.UserPremiumPlans
import net.perfectdreams.loritta.morenitta.LorittaBot
import net.perfectdreams.loritta.morenitta.dao.DonationKey
import net.perfectdreams.loritta.morenitta.dao.ServerConfig
import net.perfectdreams.loritta.morenitta.website.routes.dashboard.RequiresGuildAuthLocalizedDashboardRoute
import net.perfectdreams.loritta.morenitta.website.utils.extensions.respondHtml
import net.perfectdreams.loritta.morenitta.website.views.dashboard.guild.twitch.GuildTwitchView
import net.perfectdreams.loritta.serializable.ColorTheme
import net.perfectdreams.loritta.serializable.TwitchUser
import net.perfectdreams.loritta.serializable.config.GuildTwitchConfig
import net.perfectdreams.loritta.serializable.config.PremiumTrackTwitchAccount
import net.perfectdreams.loritta.serializable.config.TrackedTwitchAccount
import net.perfectdreams.loritta.temmiewebsession.LorittaJsonWebSession
import net.perfectdreams.temmiediscordauth.TemmieDiscordAuth
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import kotlin.math.ceil

class ConfigureTwitchRoute(loritta: LorittaBot) : RequiresGuildAuthLocalizedDashboardRoute(loritta, "/configure/twitch") {
	override suspend fun onDashboardGuildAuthenticatedRequest(call: ApplicationCall, locale: BaseLocale, i18nContext: I18nContext, discordAuth: TemmieDiscordAuth, userIdentification: LorittaJsonWebSession.UserIdentification, guild: Guild, serverConfig: ServerConfig, colorTheme: ColorTheme) {
		val (twitchAccounts, premiumTrackTwitchAccounts, valueOfTheDonationKeysEnabledOnThisGuild) = loritta.newSuspendedTransaction {
			val twitchAccounts = TrackedTwitchAccounts.selectAll().where { TrackedTwitchAccounts.guildId eq guild.idLong }
				.map {
					val state = TwitchWebUtils.getTwitchAccountTrackState(it[TrackedTwitchAccounts.twitchUserId])

					Pair(
						state,
						TrackedTwitchAccount(
							it[TrackedTwitchAccounts.id].value,
							it[TrackedTwitchAccounts.twitchUserId],
							it[TrackedTwitchAccounts.channelId],
							it[TrackedTwitchAccounts.message]
						)
					)
				}

			val premiumTrackTwitchAccounts = PremiumTrackTwitchAccounts.selectAll().where {
				PremiumTrackTwitchAccounts.guildId eq guild.idLong
			}.map {
				PremiumTrackTwitchAccount(
					it[PremiumTrackTwitchAccounts.id].value,
					it[PremiumTrackTwitchAccounts.twitchUserId]
				)
			}

			val valueOfTheDonationKeysEnabledOnThisGuild = DonationKey.find { DonationKeys.activeIn eq guild.idLong and (DonationKeys.expiresAt greaterEq System.currentTimeMillis()) }
				.toList()
				.sumOf { it.value }
				.let { ceil(it) }

			Triple(twitchAccounts, premiumTrackTwitchAccounts, valueOfTheDonationKeysEnabledOnThisGuild)
		}

		val accountsInfo = TwitchWebUtils.getCachedUsersInfoById(
			loritta,
			*((twitchAccounts.map { it.second.twitchUserId } + premiumTrackTwitchAccounts.map { it.twitchUserId }).toSet()).toLongArray()
		)

		val twitchConfig = GuildTwitchConfig(
			twitchAccounts.map { trackedTwitchAccount ->
				GuildTwitchConfig.TrackedTwitchAccountWithTwitchUserAndTrackingState(
					trackedTwitchAccount.first,
					trackedTwitchAccount.second,
					accountsInfo.firstOrNull { it.id == trackedTwitchAccount.second.twitchUserId }?.let {
						TwitchUser(it.id, it.login, it.displayName, it.profileImageUrl)
					}
				)
			},
			premiumTrackTwitchAccounts.map { trackedTwitchAccount ->
				GuildTwitchConfig.PremiumTrackTwitchAccountWithTwitchUser(
					trackedTwitchAccount,
					accountsInfo.firstOrNull { it.id == trackedTwitchAccount.twitchUserId }?.let {
						TwitchUser(it.id, it.login, it.displayName, it.profileImageUrl)
					}
				)
			}
		)

		call.respondHtml(
			GuildTwitchView(
				loritta.newWebsite!!,
				i18nContext,
				locale,
				getPathWithoutLocale(call),
				loritta.getLegacyLocaleById(locale.id),
				userIdentification,
				UserPremiumPlans.getPlanFromValue(loritta.getActiveMoneyFromDonations(userIdentification.id.toLong())),
				colorTheme,
				guild,
				"twitch",
				twitchConfig
			).generateHtml()
		)
	}
}