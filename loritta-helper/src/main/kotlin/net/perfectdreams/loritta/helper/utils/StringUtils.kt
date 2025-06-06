package net.perfectdreams.loritta.helper.utils

fun String.splitWords() =  split(Regex("\\s")).asSequence()
    .map { it.replace(Regex("[^A-Za-z]"),"").lowercase() }
    .filter { it.isNotEmpty() }