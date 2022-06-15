package com.psychedelicshayna.krypton

import org.json.JSONObject
import java.io.Serializable

class Vault : Serializable {
    companion object {
        private const val serialVersionUID: Long = 42L
    }

    class Account(
        var name: String = "",
        var entries: MutableMap<String, String> = mutableMapOf()
    ) : Serializable {
        companion object {
            private const val serialVersionUID: Long = 43L
        }

        fun contentEquals(other: Account): Boolean {
            return     name    == other.name
                    && entries == other.entries
        }
    }

    var accounts: MutableList<Account> = mutableListOf()

    fun dumpToJsonObject(): JSONObject {
        val vaultObject = JSONObject()

        for(account in accounts) {
            val entriesObject = JSONObject()

            for(entry in account.entries)
                entriesObject.put(entry.key, entry.value)

            vaultObject.put(account.name, entriesObject)
        }

        return vaultObject
    }

    fun loadFromJsonObject(jsonObject: JSONObject) {
        val newAccounts: MutableList<Account> = mutableListOf()

        for(name: String in jsonObject.keys()) {
            val newAccount = Account()
            newAccount.name = name

            val entries: JSONObject = jsonObject.getJSONObject(name)

            for (entryName: String in entries.keys()) {
                val entryValue: String = entries.getString(entryName)
                newAccount.entries[entryName] = entryValue
            }

            newAccounts.add(newAccount)
        }

        accounts = newAccounts
    }

    fun clone(): Vault =
        Vault().also { vaultCopy ->
            vaultCopy.accounts.addAll(accounts)
        }
}
