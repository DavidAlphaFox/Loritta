package net.perfectdreams.loritta.morenitta.website.routes

import com.github.salomonbrys.kotson.jsonArray
import com.github.salomonbrys.kotson.jsonObject
import io.ktor.server.application.*
import net.perfectdreams.i18nhelper.core.I18nContext
import net.perfectdreams.loritta.cinnamon.pudding.tables.DonationKeys
import net.perfectdreams.loritta.common.locale.BaseLocale
import net.perfectdreams.loritta.morenitta.LorittaBot
import net.perfectdreams.loritta.morenitta.dao.DonationKey
import net.perfectdreams.loritta.morenitta.website.utils.extensions.lorittaSession
import net.perfectdreams.loritta.morenitta.website.utils.extensions.respondHtml
import net.perfectdreams.loritta.morenitta.website.views.DonateView
import org.jetbrains.exposed.sql.and

class DonateRoute(loritta: LorittaBot) : LocalizedRoute(loritta, "/donate") {
	override val isMainClusterOnlyRoute = true

	override suspend fun onLocalizedRequest(call: ApplicationCall, locale: BaseLocale, i18nContext: I18nContext) {
		val userIdentification = call.lorittaSession.getUserIdentification(loritta.config.loritta.discord.applicationId.toLong(), loritta.config.loritta.discord.clientSecret, call)

		val keys = jsonArray()

		if (userIdentification != null) {
			val donationKeys = loritta.newSuspendedTransaction {
				// Pegar keys ativas
				DonationKey.find {
					(DonationKeys.expiresAt greaterEq System.currentTimeMillis()) and (DonationKeys.userId eq userIdentification.id.toLong())
				}.toMutableList()
			}

			for (donationKey in donationKeys) {
				keys.add(
						jsonObject(
								"id" to donationKey.id.value,
								"value" to donationKey.value,
								"expiresAt" to donationKey.expiresAt
						)
				)
			}
		}

		call.respondHtml(
			DonateView(
				loritta,
				i18nContext,
				locale,
				getPathWithoutLocale(call),
				userIdentification,
				keys
			).generateHtml()
		)
	}
}