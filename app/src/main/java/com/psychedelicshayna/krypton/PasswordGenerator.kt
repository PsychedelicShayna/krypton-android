package com.psychedelicshayna.krypton

import java.security.SecureRandom
import kotlin.random.Random
import kotlin.random.asKotlinRandom

class PasswordGenerator {
    data class CharacterClass(
        var enabled: Boolean,
        var characters: String
    )

    data class CharacterClassSpec(
        val uppercase: CharacterClass = CharacterClass(false, "ABCDEFGHIJKLMNOPQRSTUVWXYZ"),
        val lowercase: CharacterClass = CharacterClass(false, "abcdefghijklmnopqrstuvwxyz"),
        val numerical: CharacterClass = CharacterClass(false, "0123456789"),
        val special: CharacterClass = CharacterClass(false, "!#\\\$%&*+-=?@^_|"),
        val extra: CharacterClass = CharacterClass(false, "\"\\'(),./:;<>[]`{}~"),
    )

    fun generatePassword(length: Int, characterClassSpec: CharacterClassSpec): String {
        if(length <= 0) return ""

        val secureRandom: Random = SecureRandom().asKotlinRandom()

        val enabledCharacterClasses: List<String> = buildList {
            characterClassSpec.run {
                listOf(uppercase, lowercase, numerical, special, extra)
            }.forEach {
                if(it.enabled) add(it.characters)
            }
        }

        return buildString(length) {
            for(i in 1..length) append(
                enabledCharacterClasses.random(secureRandom).random(secureRandom)
            )
        }
    }
}