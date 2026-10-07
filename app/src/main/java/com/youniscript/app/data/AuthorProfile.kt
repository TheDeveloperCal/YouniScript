package com.youniscript.app.data

/** Optional local identity details used when the writer creates signed letters and books. */
data class AuthorProfile(
    val name: String = "",
    val biography: String = "",
    val signature: String = "",
    val seal: String = "",
)
