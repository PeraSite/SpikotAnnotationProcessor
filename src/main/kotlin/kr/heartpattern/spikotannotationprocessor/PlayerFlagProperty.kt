package kr.heartpattern.spikotannotationprocessor

import kotlin.reflect.KVisibility

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class PlayerFlagProperty(
    val visibility: KVisibility = KVisibility.PUBLIC,
    val name: String = ""
)