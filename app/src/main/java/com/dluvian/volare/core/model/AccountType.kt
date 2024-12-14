package com.dluvian.volare.core.model

import androidx.compose.runtime.Immutable
import rust.nostr.sdk.PublicKey

sealed class AccountType(open val publicKey: PublicKey)

@Immutable
data class PlainKeyAccount(override val publicKey: PublicKey) : AccountType(publicKey = publicKey)

@Immutable
data class BunkerAccount(override val publicKey: PublicKey) : AccountType(publicKey = publicKey)

@Immutable
data class ExternalAccount(override val publicKey: PublicKey) : AccountType(publicKey = publicKey)
