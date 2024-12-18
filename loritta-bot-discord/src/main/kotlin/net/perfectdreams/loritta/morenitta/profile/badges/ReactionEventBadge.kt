package net.perfectdreams.loritta.morenitta.profile.badges

import net.perfectdreams.i18nhelper.core.keydata.StringI18nData
import net.perfectdreams.loritta.cinnamon.pudding.Pudding
import net.perfectdreams.loritta.cinnamon.pudding.tables.reactionevents.CraftedReactionEventItems
import net.perfectdreams.loritta.cinnamon.pudding.tables.reactionevents.ReactionEventPlayers
import net.perfectdreams.loritta.morenitta.dao.Profile
import net.perfectdreams.loritta.morenitta.profile.Badge
import net.perfectdreams.loritta.morenitta.profile.ProfileDesignManager
import net.perfectdreams.loritta.morenitta.profile.ProfileUserInfoData
import net.perfectdreams.loritta.morenitta.reactionevents.ReactionEvent
import net.perfectdreams.loritta.morenitta.reactionevents.events.Christmas2024ReactionEvent
import net.perfectdreams.loritta.morenitta.reactionevents.events.Halloween2024ReactionEvent
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.util.*

sealed class ReactionEventBadge(
	val pudding: Pudding,
	id: UUID,
	title: StringI18nData,
	description: StringI18nData,
	badgeName: String,
	priority: Int,
	reactionEvent: ReactionEvent,
	val requiredPoints: Int
) : Badge.LorittaBadge(
	id,
	title,
	description,
	badgeName,
	priority
) {
	class Halloween2024ReactionEventBadge(pudding: Pudding) : ReactionEventBadge(
		pudding,
		UUID.fromString("e9de17d4-8ee6-4f18-b8a0-ef9087b5ec43"),
		ProfileDesignManager.I18N_BADGES_PREFIX.Halloween2024.Title,
		ProfileDesignManager.I18N_BADGES_PREFIX.Halloween2024.Description,
		"halloween2019.png",
		100,
		Halloween2024ReactionEvent,
		10
	)

	class Halloween2024ReactionEventSuperBadge(pudding: Pudding) : ReactionEventBadge(
		pudding,
		UUID.fromString("01b1ee02-53ee-4a2c-a094-8c1d6b40c680"),
		ProfileDesignManager.I18N_BADGES_PREFIX.SuperHalloween2024.Title,
		ProfileDesignManager.I18N_BADGES_PREFIX.SuperHalloween2024.Description,
		"halloween2024_super.png",
		100,
		Halloween2024ReactionEvent,
		1_000
	)

	class Christmas2024ReactionEventBadge(pudding: Pudding) : ReactionEventBadge(
		pudding,
		UUID.fromString("3e5913fd-26d1-4b0a-88e3-0074d138f2ff"),
		ProfileDesignManager.I18N_BADGES_PREFIX.Christmas2024.Title,
		ProfileDesignManager.I18N_BADGES_PREFIX.Christmas2024.Description,
		"christmas2024.png",
		100,
		Christmas2024ReactionEvent,
		10
	)

	class Christmas2024ReactionEventSuperBadge(pudding: Pudding) : ReactionEventBadge(
		pudding,
		UUID.fromString("c4d78a28-0028-48e2-9363-4df1256fca5a"),
		ProfileDesignManager.I18N_BADGES_PREFIX.SuperChristmas2024.Title,
		ProfileDesignManager.I18N_BADGES_PREFIX.SuperChristmas2024.Description,
		"christmas2024_super.png",
		100,
		Christmas2024ReactionEvent,
		700
	)

	private val eventInternalId = reactionEvent.internalId

	override suspend fun checkIfUserDeservesBadge(user: ProfileUserInfoData, profile: Profile, mutualGuilds: Set<Long>): Boolean {
		return pudding.transaction {
			CraftedReactionEventItems.innerJoin(ReactionEventPlayers).selectAll()
				.where {
					CraftedReactionEventItems.event eq eventInternalId and (ReactionEventPlayers.userId eq user.id)
				}
				.count() >= requiredPoints
		}
	}
}