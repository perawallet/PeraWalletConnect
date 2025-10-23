package app.perawallet.walletconnectv2.internal.utils

import app.perawallet.walletconnectv2.internal.common.model.AccountId
import app.perawallet.walletconnectv2.foundation.common.model.Topic

private const val SELF_PARTICIPANT_CONTEXT = "self_participant/"
private const val SELF_INVITE_PUBLIC_KEY_CONTEXT = "self_inviteKey/"
private const val SELF_IDENTITY_PUBLIC_KEY_CONTEXT = "self_identityKey/"
private const val SELF_PEER_PUBLIC_KEY_CONTEXT = "self_peerKey/"

fun AccountId.getInviteTag(): String = "$SELF_INVITE_PUBLIC_KEY_CONTEXT${this.value}"
fun Topic.getParticipantTag(): String = "$SELF_PARTICIPANT_CONTEXT${this.value}"
fun AccountId.getIdentityTag(): String = "$SELF_IDENTITY_PUBLIC_KEY_CONTEXT${this.value}"
fun Pair<AccountId, Topic>.getPeerTag(): String = "$SELF_PEER_PUBLIC_KEY_CONTEXT${this.first.value}/${this.second.value}"
