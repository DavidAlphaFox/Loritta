package net.perfectdreams.loritta.morenitta.website.routes.api.v1.user

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import net.dv8tion.jda.api.entities.User.UserFlag
import net.perfectdreams.loritta.morenitta.LorittaBot
import net.perfectdreams.loritta.morenitta.profile.ProfileUserInfoData
import net.perfectdreams.loritta.morenitta.profile.profiles.AnimatedProfileCreator
import net.perfectdreams.loritta.morenitta.profile.profiles.RawProfileCreator
import net.perfectdreams.loritta.morenitta.profile.profiles.StaticProfileCreator
import net.perfectdreams.loritta.morenitta.utils.ImageFormat
import net.perfectdreams.loritta.morenitta.website.routes.api.v1.RequiresAPIDiscordLoginRoute
import net.perfectdreams.loritta.serializable.BackgroundStorageType
import net.perfectdreams.loritta.temmiewebsession.LorittaJsonWebSession
import net.perfectdreams.temmiediscordauth.TemmieDiscordAuth
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.*
import javax.imageio.ImageIO

class GetSelfUserProfileRoute(loritta: LorittaBot) : RequiresAPIDiscordLoginRoute(loritta, "/api/v1/users/@me/profile") {
	override suspend fun onAuthenticatedRequest(call: ApplicationCall, discordAuth: TemmieDiscordAuth, userIdentification: LorittaJsonWebSession.UserIdentification) {
		val profile = loritta.getOrCreateLorittaProfile(userIdentification.id)
		val settings = loritta.newSuspendedTransaction {
			profile.settings
		}

		val internalTypeName = call.parameters["type"] ?: "defaultDark"
		val backgroundTypeName = call.parameters["background"]

		val profileCreator = loritta.profileDesignManager.designs.first {
			it.internalName == internalTypeName
		}

		val locale = loritta.localeManager.getLocaleById("default")

		val userId = userIdentification.id.toLong()
		// Disabled because Loritta loads the avatar URL from the user identification cache
		// This causes issues when loading the "profile" page when the user changed the avatar recently
		// So for now we are going to always load Discord's default avatar
		/* val avatarUrl = if (userIdentification.avatar != null) {
			val extension = if (userIdentification.avatar.startsWith("a_")) { // Avatares animados no Discord começam com "_a"
				"gif"
			} else { "png" }

			"https://cdn.discordapp.com/avatars/${userId}/${userIdentification.avatar}.${extension}?size=256"
		} else {
			val avatarId = userId % 5

			"https://cdn.discordapp.com/embed/avatars/$avatarId.png?size=256"
		} */
		val avatarUrl = run {
			val avatarId = userId % 5

			"https://cdn.discordapp.com/embed/avatars/$avatarId.png?size=256"
		}

		val backgroundImage = if (backgroundTypeName == null) {
			BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
		} else {
			val dssNamespace = loritta.dreamStorageService.getCachedNamespaceOrRetrieve()

			val background = loritta.pudding.backgrounds.getBackground(backgroundTypeName)
			if (background == null) {
				call.respondText("", status = HttpStatusCode.NotFound)
				return
			}

			val variation = background.getVariationForProfileDesign(internalTypeName)
			val url = when (variation.storageType) {
				BackgroundStorageType.DREAM_STORAGE_SERVICE -> loritta.profileDesignManager.getDreamStorageServiceBackgroundUrlWithCropParameters(
					loritta.config.loritta.dreamStorageService.url,
					dssNamespace,
					variation
				)

				BackgroundStorageType.ETHEREAL_GAMBI -> loritta.profileDesignManager.getEtherealGambiBackgroundUrl(
					variation
				)
			}

			// We could just respondRedirect the URL, but we need to scale the image because some images don't have the proper aspect ratio
			// (Yes, even tho the DreamStorageService has custom crops on the URL... it, for some reason, aren't actually correct?)
			val backgroundImage = BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB)
			val downloadedBackgroundImage = ImageIO.read(loritta.http.get(url).readBytes().inputStream()) // We hope this is a trusted image
			backgroundImage.createGraphics().drawImage(downloadedBackgroundImage, 0, 0, 800, 600, null)
			backgroundImage
		}

		val senderUserData = ProfileUserInfoData(
			userIdentification.id.toLong(),
			userIdentification.username,
			userIdentification.discriminator,
			avatarUrl,
			false,
			EnumSet.noneOf(UserFlag::class.java) // TODO: Fix user flags, afaik they are provided in the UserIdentification
		)

		val image = when (profileCreator) {
			is StaticProfileCreator -> profileCreator.create(
				senderUserData,
				senderUserData,
				profile,
				null,
				listOf(),
				listOf(),
				null,
				locale,
				loritta.languageManager.defaultI18nContext, // TODO: Provide the correct i18n context!
				backgroundImage, // Create profile with transparent background
				settings.aboutMe ?: "???",
				listOf()
			)
			is AnimatedProfileCreator -> profileCreator.create(
				senderUserData,
				senderUserData,
				profile,
				null,
				listOf(),
				listOf(),
				null,
				locale,
				loritta.languageManager.defaultI18nContext, // TODO: Provide the correct i18n context!
				backgroundImage, // Create profile with transparent background
				settings.aboutMe ?: "???",
				listOf()
			).first() // We only want the first frame of the list
			is RawProfileCreator -> {
				// TODO: We need to refactor RawProfileCreator to properly support this endpoint
				// This is a special case because idk how we could support this endpoint with "RawProfileCreator"
				val profileImageRawData = profileCreator.create(
					senderUserData,
					senderUserData,
					profile,
					null,
					listOf(),
					listOf(),
					null,
					locale,
					loritta.languageManager.defaultI18nContext, // TODO: Provide the correct i18n context!
					backgroundImage, // Create profile with transparent background
					settings.aboutMe ?: "???",
					listOf()
				)

				call.respondBytes(
					profileImageRawData.first,
					when (profileImageRawData.second) {
						ImageFormat.PNG -> ContentType.Image.PNG
						ImageFormat.JPG -> ContentType.Image.JPEG
						ImageFormat.GIF -> ContentType.Image.GIF
						// Ktor does not have ContentType.Image.WEBP yet
						ImageFormat.WEBP -> ContentType.parse("image/webp")
					},
					HttpStatusCode.OK
				)
				return
			}
			else -> error("Unsupported Profile Creator Type $profileCreator")
		}

		val baos = ByteArrayOutputStream()
		ImageIO.write(image, "png", baos)
		call.respondBytes(baos.toByteArray(), ContentType.Image.PNG, HttpStatusCode.OK)
	}
}