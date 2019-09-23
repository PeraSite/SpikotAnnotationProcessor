package io.github.readymadeprogrammer.spikotannotationprocessor

import com.google.auto.service.AutoService
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.StandardLocation

@AutoService(Processor::class)
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class SpikotAnnotationProcessor : AbstractProcessor() {
    private fun getTypeElement(name: String): TypeElement {
        return processingEnv.elementUtils.getTypeElement(name)
    }

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        // Verify parameters
        verifyParameter(
            roundEnv,
            getTypeElement("io.github.ReadyMadeProgrammer.Spikot.packet.PacketHandler"),
            listOf(
                getTypeElement("com.comphenix.protocol.events.PacketEvent")
            ),
            "Method annotated with @PacketHandler should have PacketEvent as parameter"
        )
        verifyParameter(
            roundEnv,
            getTypeElement("org.bukkit.event.EventHandler"),
            listOf(
                getTypeElement("org.bukkit.event.Event")
            ),
            "Method annotated with @EventHandler should have Event as parameter"
        )

        //Generate for @PlayerProperty
        val playerProperty = getTypeElement("io.github.readymadeprogrammer.spikotannotationprocessor.PlayerProperty")
        for (element in roundEnv.getElementsAnnotatedWith(playerProperty)) {
            if (element !is TypeElement) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "PlayerProperty can only annotate type")
                continue
            }
            val packageElement = processingEnv.elementUtils.getPackageOf(element)
            val className = element.simpleName
            val annotation = element.annotationMirrors
                .find { it.annotationType.asElement() == playerProperty }!!
            val name = annotation.elementValues.entries
                .find { it.key.simpleName.toString() == "name" }?.value?.value as? String
                ?: run {
                    val tmp = if (className.endsWith("Property")) className.substring(0, className.length - 8)
                    else className
                    if (tmp[0].isUpperCase())
                        tmp[0].toLowerCase() + tmp.substring(1)
                    else
                        tmp
                }.toString()

            val visibility = annotation.elementValues.entries
                .find { it.key.simpleName.toString() == "visibility" }!!.value.value.toString().toLowerCase()

            val targetType = getTypeElement(
                wrapJavaPrimitive(
                    annotation.elementValues.entries
                        .find { it.key.simpleName.toString() == "type" }!!.value.value.toString()
                )
            ).qualifiedName.toString()

            processingEnv.filer.createResource(
                StandardLocation.SOURCE_OUTPUT,
                packageElement.qualifiedName.toString(),
                "${name[0].toUpperCase() + name.substring(1)}PlayerPropertyAccessor.kt",
                element
            ).openWriter().use {
                it.write(
                    """package ${packageElement.qualifiedName}
                        |import org.bukkit.entity.Player
                        |import io.github.ReadyMadeProgrammer.Spikot.utils.*
                        |
                        |$visibility var Player.$name: ${convertKotlinType(targetType)}?
                        |   get() = player[${className}]
                        |   set(value){
                        |       if(value == null){
                        |           player.remove(${className})
                        |       } else {
                        |           player[${className}] = value
                        |       }
                        |   }
                    """.trimMargin()
                )
            }
        }
        return true
    }

    private fun wrapJavaPrimitive(name: String): String = when (name) {
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

    private fun convertKotlinType(name: String): String = when {
        name == "java.lang.Boolean" -> "Boolean"
        name == "java.lang.Byte" -> "Byte"
        name == "java.lang.Short" -> "Short"
        name == "java.lang.Character" -> "Char"
        name == "java.lang.Integer" -> "Int"
        name == "java.lang.Long" -> "Long"
        name == "java.lang.Float" -> "Float"
        name == "java.lang.Double" -> "Double"
        name.endsWith("[]") -> "Array<${convertKotlinType(name.substring(0, name.length - 2))}>"
        else -> name
    }

    private fun verifyParameter(
        roundEnv: RoundEnvironment,
        annotation: TypeElement,
        parameter: List<TypeElement>,
        error: String
    ) {
        roundEnv.getElementsAnnotatedWith(annotation).forEach { element ->
            if (element.kind != ElementKind.METHOD) {
                return
            }
            val actual = (element as ExecutableElement).parameters
            if (parameter.size != actual.size) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, error, element)
                return
            }
            for (i in parameter.indices) {
                if (!processingEnv.typeUtils.isSubtype(
                        actual[i].asType(),
                        parameter[i].asType()
                    )
                ) {
                    processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, error, element)
                    return
                }
            }
        }
    }
}