package com.psychedelicshayna.krypton

import java.io.Serializable

class VaultAccount(
    val AccountName: String = "",
    val AccountEntries: Map<String, String> = mapOf()
) : Serializable {

}