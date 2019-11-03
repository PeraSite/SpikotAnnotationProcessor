package kr.heartpattern.spikotannotationprocessor

import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.TypeElement


fun getTypeElement(processingEnv: ProcessingEnvironment, name: String): TypeElement {
    return processingEnv.elementUtils.getTypeElement(name)
}

fun wrapJavaPrimitive(name: String): String = when (name) {
    "boolean" -> "java.lang.Boolean"
    "byte" -> "java.lang.Byte"
    "short" -> "java.lang.Short"
    "char" -> "java.lang.Character"
    "int" -> "java.lang.Integer"
    "long" -> "java.lang.Long"
    "float" -> "java.lang.Float"
    "double" -> "java.lang.Double"
    else -> name
}

fun convertKotlinType(name: String): String = when {
    name == "java.lang.Boolean" -> "Boolean"
    name == "java.lang.Byte" -> "Byte"
    name == "java.lang.Short" -> "Short"
    name == "java.lang.Character" -> "Char"
    name == "java.lang.Integer" -> "Int"
    name == "java.lang.Long" -> "Long"
    name == "java.lang.Float" -> "Float"
    name == "java.lang.Double" -> "Double"
    name.endsWith("[]") -> "Array<${convertKotlinType(
        name.substring(
            0,
            name.length - 2
        )
    )}>"
    else -> name
}