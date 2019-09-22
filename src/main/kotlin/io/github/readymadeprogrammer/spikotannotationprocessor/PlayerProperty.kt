package io.github.readymadeprogrammer.spikotannotationprocessor

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class PlayerProperty(
    val name: String = ""
)