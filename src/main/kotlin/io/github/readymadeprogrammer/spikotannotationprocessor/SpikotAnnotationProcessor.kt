package io.github.readymadeprogrammer.spikotannotationprocessor

import com.google.auto.service.AutoService
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
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

            val targetType = (resolvePlayerPropertyType(element.superclass)
                ?: element.interfaces.asSequence()
                    .map(::resolvePlayerPropertyType)
                    .filterNotNull()
                    .first()).qualifiedName.toString()

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
                        |var Player.$name: ${convertKotlinType(targetType)}?
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

    private fun convertKotlinType(name: String): String = when{
        name == "java.lang.Boolean" -> "Boolean"
        name == "java.lang.Byte" -> "Byte"
        name == "java.lang.Short" -> "Short"
        name == "java.lang.Character" -> "Char"
        name == "java.lang.Integer" -> "Int"
        name == "java.lang.Long" -> "Long"
        name == "java.lang.Float" -> "Float"
        name == "java.lang.Double" -> "Double"
        name.endsWith("[]") -> "Array<${convertKotlinType(name.substring(0,name.length-2))}>"
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

    private fun resolvePlayerPropertyType(mirror: TypeMirror?): TypeElement? {
        if (mirror == null)
            return null
        if (processingEnv.typeUtils.isSameType(
                processingEnv.typeUtils.erasure(mirror),
                getTypeElement("io.github.ReadyMadeProgrammer.Spikot.misc.Property").asType()
            )
        ) {
            return getTypeElement((mirror as DeclaredType).typeArguments[0].toString())
        }
        val genericAnnotation = GenericPropertyType::class.qualifiedName
        val staticAnnotation = StaticPropertyType::class.qualifiedName
        val annotations = (processingEnv.typeUtils.asElement(mirror) as TypeElement).annotationMirrors
        val generic =
            annotations.find { (it.annotationType.asElement() as TypeElement).qualifiedName.toString() == genericAnnotation}
        val static =
            annotations.find { (it.annotationType.asElement() as TypeElement).qualifiedName.toString() == staticAnnotation }
        if (generic != null) {
            val index = generic.elementValues.entries
                .find { it.key.simpleName.toString() == "value" }!!
                .value.value as Int
            val type = mirror as DeclaredType
            return getTypeElement(type.typeArguments[index].toString())
        }
        if (static != null) {
            return getTypeElement(
                static.elementValues.entries
                    .find { it.key.simpleName.toString() == "value" }!!.value.value.toString()
            )
        }
        return null
    }
}