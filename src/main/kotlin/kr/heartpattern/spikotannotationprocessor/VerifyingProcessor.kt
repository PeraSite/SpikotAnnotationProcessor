package kr.heartpattern.spikotannotationprocessor

import com.google.auto.service.AutoService
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

@AutoService(Processor::class)
@SupportedAnnotationTypes("kr.heartpattern.spikot.packet.PacketHandler","org.bukkit.event.EventHandler")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class VerifyingProcessor : AbstractProcessor() {
    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        // Verify parameters
        var result = verifyParameter(
            roundEnv,
            getTypeElement(
                processingEnv,
                "kr.heartpattern.spikot.packet.PacketHandler"
            ),
            listOf(
                getTypeElement(
                    processingEnv,
                    "com.comphenix.protocol.events.PacketEvent"
                )
            ),
            "Method annotated with @PacketHandler should have PacketEvent as parameter"
        )
        result = result && verifyParameter(
            roundEnv,
            getTypeElement(processingEnv, "org.bukkit.event.EventHandler"),
            listOf(
                getTypeElement(processingEnv, "org.bukkit.event.Event")
            ),
            "Method annotated with @EventHandler should have Event as parameter"
        )
        return result
    }

    private fun verifyParameter(
        roundEnv: RoundEnvironment,
        annotation: TypeElement,
        parameter: List<TypeElement>,
        error: String
    ): Boolean {
        roundEnv.getElementsAnnotatedWith(annotation).forEach { element ->
            if (element.kind != ElementKind.METHOD) {
                return@forEach
            }
            val actual = (element as ExecutableElement).parameters
            if (parameter.size != actual.size) {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, error, element)
                return false
            }
            for (i in parameter.indices) {
                if (!processingEnv.typeUtils.isSubtype(
                        actual[i].asType(),
                        parameter[i].asType()
                    )
                ) {
                    processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, error, element)
                    return false
                }
            }
        }
        return true
    }
}