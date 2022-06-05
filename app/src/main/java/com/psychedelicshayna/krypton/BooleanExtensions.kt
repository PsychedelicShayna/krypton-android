package com.psychedelicshayna.krypton

fun Boolean.and(condition: (Boolean) -> Boolean): Boolean {
    return this && condition.invoke(this)
}

fun Boolean.or(condition: (Boolean) -> Boolean): Boolean {
    return this || condition.invoke(this)
}

fun Boolean.xor(condition: (Boolean) -> Boolean): Boolean {
    return this xor condition.invoke(this)
}

fun <T> Boolean.letIf(value: Boolean, let: () -> (T)): T? {
    return if(this == value) { let.invoke() } else { null }
}
