package kr.heartpattern.spikotannotationprocessor

import com.google.auto.service.AutoService
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.StandardLocation

@AutoService(Processor::class)
@SupportedAnnotationTypes("kr.heartpattern.spikotannotationprocessor.PlayerFlagProperty")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class PlayerFlagPropertyProcessor : AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        //Generate for @PlayerProperty
        val playerProperty =
            getTypeElement(
                processingEnv,
                "kr.heartpattern.spikotannotationprocessor.PlayerFlagProperty"
            )
        for (element in roundEnv.getElementsAnnotatedWith(playerProperty)) {
            if (element !is TypeElement) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "PlayerProperty can only annotate type")
                return false
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
                    if (tmp[0].isLowerCase())
                        "is" + tmp[0].toUpperCase() + tmp.substring(1)
                    else
                        "is$tmp"
                }.toString()

            val visibility = annotation.elementValues.entries
                .find { it.key.simpleName.toString() == "visibility" }?.value?.value?.toString()?.toLowerCase() ?: ""

            processingEnv.filer.createResource(
                StandardLocation.SOURCE_OUTPUT,
                packageElement.qualifiedName.toString(),
                "${name[0].toUpperCase() + name.substring(1)}PlayerPropertyAccessor.kt",
                element
            ).openWriter().use {
                it.write(
                    """package ${packageElement.qualifiedName}
                            |import org.bukkit.entity.Player
                            |import kr.heartpattern.spikot.utils.*
                            |
                            |$visibility var Player.$name: Boolean
                            |   get() = (player[${className}] != null)
                            |   set(value){
                            |       if(!value){
                            |           player.remove(${className})
                            |       } else {
                            |           player[${className}] = Unit
                            |       }
                            |   }
                        """.trimMargin()
                )
            }
        }
        return true
    }
}