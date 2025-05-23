package net.perfectdreams.loritta.morenitta.websiteinternal.loripublicapi.v1.guilds

import dev.minn.jda.ktx.messages.MessageCreateBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.util.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.LongAsStringSerializer
import kotlinx.serialization.json.Json
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.UserSnowflake
import net.perfectdreams.loritta.cinnamon.discord.interactions.commands.styled
import net.perfectdreams.loritta.cinnamon.emotes.Emotes
import net.perfectdreams.loritta.cinnamon.pudding.tables.ThirdPartySonhosTransferRequests
import net.perfectdreams.loritta.common.utils.TokenType
import net.perfectdreams.loritta.common.utils.UserPremiumPlans
import net.perfectdreams.loritta.i18n.I18nKeysData
import net.perfectdreams.loritta.morenitta.LorittaBot
import net.perfectdreams.loritta.morenitta.interactions.vanilla.economy.SonhosPayExecutor
import net.perfectdreams.loritta.morenitta.utils.AccountUtils
import net.perfectdreams.loritta.morenitta.utils.ThirdPartySonhosTransferUtils
import net.perfectdreams.loritta.morenitta.utils.extensions.await
import net.perfectdreams.loritta.morenitta.utils.extensions.getGuildMessageChannelById
import net.perfectdreams.loritta.morenitta.utils.stripCodeMarks
import net.perfectdreams.loritta.morenitta.website.utils.extensions.respondJson
import net.perfectdreams.loritta.morenitta.websiteinternal.loripublicapi.*
import net.perfectdreams.loritta.publichttpapi.LoriPublicHttpApiEndpoints
import net.perfectdreams.loritta.serializable.UserId
import org.jetbrains.exposed.sql.insert
import java.time.Instant
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class PostRequestSonhosRoute(m: LorittaBot) : LoriPublicAPIGuildRoute(
    m,
    LoriPublicHttpApiEndpoints.REQUEST_SONHOS,
    RateLimitOptions(
        5,
        5.seconds
    )
) {
    override suspend fun onGuildAPIRequest(call: ApplicationCall, tokenInfo: TokenInfo, guild: Guild, member: Member) {
        if (!member.hasPermission(Permission.ADMINISTRATOR)) {
            call.respondJson("", status = HttpStatusCode.Unauthorized)
            return
        }

        if (tokenInfo.tokenType != TokenType.BOT) {
            call.respondJson(Json.encodeToString(GenericErrorResponse("Only bots can use this endpoint!")), status = HttpStatusCode.BadRequest)
            return
        }

        val request = Json.decodeFromString<RequestSonhosRequest>(call.receiveText())
        if (request.senderId != tokenInfo.creatorId) {
            val premium = UserPremiumPlans.getPlanFromValue(m.getActiveMoneyFromDonations(tokenInfo.creatorId))
            if (!premium.sonhosAPIAccess) {
                call.respondJson(
                    Json.encodeToString(
                        GenericErrorResponse(
                            "The token owner needs to have the Loritta \"Essential\" Premium Plan to be able to use sonhos transfers!"
                        )
                    ),
                    status = HttpStatusCode.PaymentRequired
                )
                return
            }
        }

        val senderSnowflake = UserSnowflake.fromId(request.senderId)
        val channel = guild.getGuildMessageChannelById(call.parameters.getOrFail("channelId"))
        if (channel == null) {
            call.respondJson(
                Json.encodeToString(
                    GenericErrorResponse(
                        "Unknown Channel"
                    )
                ),
                status = HttpStatusCode.BadRequest
            )
            return
        }

        // These checks should be the SAME checks used in SonhosPayExecutor!
        val howMuch = request.quantity
        if (0L >= howMuch) {
            call.respondJson(
                Json.encodeToString(
                    GenericErrorResponse(
                        "You can't request zero or negative sonhos!"
                    )
                ),
                status = HttpStatusCode.BadRequest
            )
            return
        }

        if (member.idLong == request.senderId) {
            call.respondJson(
                Json.encodeToString(
                    GenericErrorResponse(
                        "You can't request sonhos from yourself!"
                    )
                ),
                status = HttpStatusCode.BadRequest
            )
            return
        }

        if (request.expiresAfterMillis !in SonhosPayExecutor.TIME_TO_LIVE_RANGE) {
            call.respondJson(
                Json.encodeToString(
                    GenericErrorResponse(
                        "expiresAfterMillis is outside of allowed TTL range! (${SonhosPayExecutor.TIME_TO_LIVE_RANGE})"
                    )
                ),
                status = HttpStatusCode.BadRequest
            )
            return
        }

        if (request.reason.isBlank()) {
            call.respondJson(
                Json.encodeToString(
                    GenericErrorResponse(
                        "Reason cannot be blank!"
                    )
                ),
                status = HttpStatusCode.BadRequest
            )
            return
        }

        if (request.reason.length !in 1..100) {
            call.respondJson(
                Json.encodeToString(
                    GenericErrorResponse(
                        "Reason must be between 1..100!"
                    )
                ),
                status = HttpStatusCode.BadRequest
            )
            return
        }

        val senderProfile = m.getLorittaProfile(request.senderId)

        if (senderProfile == null || howMuch > senderProfile.money) {
            call.respondJson(
                Json.encodeToString(
                    GenericErrorResponse(
                        "Sender does not have enough sonhos!"
                    )
                ),
                status = HttpStatusCode.BadRequest
            )
            return
        }

        val senderAccountOldEnoughResult = SonhosPayExecutor.checkIfAccountIsOldEnoughToSendSonhos(senderSnowflake)

        when (senderAccountOldEnoughResult) {
            is SonhosPayExecutor.Companion.OtherAccountOldEnoughResult.NotOldEnough -> {
                call.respondJson(
                    Json.encodeToString(
                        GenericErrorResponse(
                            "Sender account is not old enough!"
                        )
                    ),
                    status = HttpStatusCode.BadRequest
                )
                return
            }

            SonhosPayExecutor.Companion.OtherAccountOldEnoughResult.Success -> {}
        }

        val senderAccountGotDailyAtLeastOnceResult = SonhosPayExecutor.checkIfAccountGotDailyAtLeastOnce(m, senderSnowflake)

        when (senderAccountGotDailyAtLeastOnceResult) {
            SonhosPayExecutor.Companion.AccountGotDailyAtLeastOnceResult.HaventGotDailyOnce -> {
                call.respondJson(
                    Json.encodeToString(
                        GenericErrorResponse(
                            "Sender account has not received daily at least once!"
                        )
                    ),
                    status = HttpStatusCode.BadRequest
                )
                return
            }
            SonhosPayExecutor.Companion.AccountGotDailyAtLeastOnceResult.Success -> {}
        }

        // Check if the user got the daily reward today
        val todayDailyReward = AccountUtils.getUserTodayDailyReward(m, senderProfile)
        if (todayDailyReward == null) {
            call.respondJson(
                Json.encodeToString(
                    GenericErrorResponse(
                        "Sender has not received daily reward today!"
                    )
                ),
                status = HttpStatusCode.BadRequest
            )
            return
        }

        // Check if the user is banned from using Loritta
        val userBannedState = m.pudding.users.getUserBannedState(UserId(request.senderId))

        if (userBannedState != null) {
            call.respondJson(
                Json.encodeToString(
                    GenericErrorResponse(
                        "Sender is Loritta Banned!"
                    )
                ),
                status = HttpStatusCode.BadRequest
            )
            return
        }

        if (senderProfile != null) {
            val vacationUntil = senderProfile.vacationUntil
            if (vacationUntil != null && vacationUntil > Instant.now()) {
                // Yeah, they are!
                call.respondJson(
                    Json.encodeToString(
                        GenericErrorResponse(
                            "Sender is on vacation mode!"
                        )
                    ),
                    status = HttpStatusCode.BadRequest
                )
                return
            }
        }

        val now = Instant.now()
        val nowPlusTimeToLive = now.plusMillis(request.expiresAfterMillis)

        // Load the server config because we need the i18nContext
        val serverConfig = m.getOrCreateServerConfig(guild.idLong)
        val i18nContext = m.languageManager.getI18nContextByLegacyLocaleId(serverConfig.localeId)
        val userPremiumPlan = UserPremiumPlans.getPlanFromValue(m.getActiveMoneyFromDonations(senderSnowflake.idLong))

        val hasTax = userPremiumPlan.thirdPartySonhosTransferTax != 0.0
        val tax = (request.quantity * userPremiumPlan.thirdPartySonhosTransferTax).toLong()

        if (tax == 0L && hasTax) {
            // Uh oh!
            call.respondJson(
                Json.encodeToString(
                    GenericErrorResponse(
                        "You need to transfer more sonhos, because the tax would be zero!"
                    )
                ),
                status = HttpStatusCode.BadRequest
            )
            return
        }

        // Attempt to initiate a transfer
        val sonhosTransferRequest = m.transaction {
            ThirdPartySonhosTransferRequests.insert {
                it[ThirdPartySonhosTransferRequests.tokenUser] = tokenInfo.userId
                it[ThirdPartySonhosTransferRequests.giver] = request.senderId
                it[ThirdPartySonhosTransferRequests.receiver] = member.idLong
                it[ThirdPartySonhosTransferRequests.quantity] = request.quantity
                it[ThirdPartySonhosTransferRequests.requestedAt] = now
                it[ThirdPartySonhosTransferRequests.expiresAt] = nowPlusTimeToLive
                it[ThirdPartySonhosTransferRequests.receiverAcceptedAt] = now // The bot should automatically accept the transfer request
                it[ThirdPartySonhosTransferRequests.reason] = request.reason
                it[ThirdPartySonhosTransferRequests.tax] = tax
                it[ThirdPartySonhosTransferRequests.taxPercentage] = userPremiumPlan.thirdPartySonhosTransferTax
            }
        }
        val sonhosTransferRequestId = sonhosTransferRequest[ThirdPartySonhosTransferRequests.id]

        // Attempt to send the message
        val messageId = channel.sendMessage(
            MessageCreateBuilder {
                mentions {
                    user(senderSnowflake)
                }

                styled(
                    "**${i18nContext.get(I18nKeysData.Commands.Command.Pay.ApiInitiatedRequest(member.asMention, senderSnowflake.asMention))}**",
                    Emotes.LoriMegaphone
                )

                styled(
                    i18nContext.get(I18nKeysData.Commands.Command.Pay.ApiInitiatedTransferReason("`${request.reason.stripCodeMarks()}`")),
                    Emotes.PageFacingUp
                )

                val message = ThirdPartySonhosTransferUtils.createSonhosTransferMessageThirdPerson(
                    i18nContext,
                    senderSnowflake,
                    member,
                    howMuch,
                    tax,
                    nowPlusTimeToLive,
                    sonhosTransferRequestId.value,
                    1 // The receiver (ourselves) should ALWAYS have the transfer pre-accepted!
                )

                message.invoke(this)
            }.build()
        ).await()

        call.respondJson(
            Json.encodeToString(
                RequestSonhosResponse(
                    sonhosTransferRequestId.value,
                    messageId.idLong,
                    sonhosTransferRequest[ThirdPartySonhosTransferRequests.quantity],
                    sonhosTransferRequest[ThirdPartySonhosTransferRequests.quantity] - sonhosTransferRequest[ThirdPartySonhosTransferRequests.tax],
                    sonhosTransferRequest[ThirdPartySonhosTransferRequests.tax],
                    sonhosTransferRequest[ThirdPartySonhosTransferRequests.taxPercentage],
                )
            ),
            status = HttpStatusCode.OK
        )
        return
    }

    @Serializable
    data class RequestSonhosRequest(
        @LoriPublicAPIParameter
        @Serializable(LongAsStringSerializer::class)
        val senderId: Long,
        @LoriPublicAPIParameter
        val quantity: Long,
        @LoriPublicAPIParameter
        val reason: String,
        @LoriPublicAPIParameter
        val expiresAfterMillis: Long = 15.minutes.inWholeMilliseconds
    )

    @Serializable
    data class RequestSonhosResponse(
        @LoriPublicAPIParameter
        @Serializable(LongAsStringSerializer::class)
        val sonhosTransferRequestId: Long,
        @LoriPublicAPIParameter
        @Serializable(LongAsStringSerializer::class)
        val messageId: Long,
        @LoriPublicAPIParameter
        val quantity: Long,
        @LoriPublicAPIParameter
        val quantityAfterTax: Long,
        @LoriPublicAPIParameter
        val tax: Long,
        @LoriPublicAPIParameter
        val taxPercentage: Double
    )
}
