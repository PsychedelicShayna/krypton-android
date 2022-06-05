package com.psychedelicshayna.krypton

import java.io.Serializable

class VaultAccount(
    val AccountName: String = "",
    val AccountEntries: Map<String, String> = mapOf()
) : Serializable {
    fun contentEquals(other: VaultAccount): Boolean {
        return     AccountName    == other.AccountName
                && AccountEntries == other.AccountEntries
    }
}