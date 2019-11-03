package kr.heartpattern.spikotannotationprocessor

import kotlin.reflect.KClass
import kotlin.reflect.KVisibility

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class PlayerProperty(
    val type: KClass<*>,
    val visibility: KVisibility = KVisibility.PUBLIC,
    val name: String = "",
    val nullable: Boolean = true
)