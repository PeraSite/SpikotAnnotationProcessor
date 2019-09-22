package io.github.readymadeprogrammer.spikotannotationprocessor

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class GenericPropertyType(
    val value: Int
)

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class StaticPropertyType(
    val value: KClass<*>
)