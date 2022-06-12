package com.psychedelicshayna.krypton

import java.io.Serializable

class VaultAccount(
    var AccountName: String = "",
    var AccountEntries: Map<String, String> = mapOf()
) : Serializable {
    fun contentEquals(other: VaultAccount): Boolean {
        return     AccountName    == other.AccountName
                && AccountEntries == other.AccountEntries
    }
}