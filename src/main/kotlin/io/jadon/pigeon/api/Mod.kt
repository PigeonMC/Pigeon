package io.jadon.pigeon.api

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Mod(
        val id: String,
        val author: String,
        val version: String
)

data class ModInfo(
        var id: String,
        var className: String
)

data class ModContainer<T>(
        val modClass: Class<T>,
        val modInfo: ModInfo
)
