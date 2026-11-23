package com.deployagram.demo.appa

import kotlinx.serialization.Serializable

/** A book that BookOverflow suggests, as returned by GET /BookOverflow/suggestion?email=... */
@Serializable
data class BookSuggestion(
    val name: String,
    val edition: String,
    val format: String,
    val authors: List<String>,
) {
    fun describe(): String = "$name ($edition edition, $format)\nby ${authors.joinToString(", ")}"
}
