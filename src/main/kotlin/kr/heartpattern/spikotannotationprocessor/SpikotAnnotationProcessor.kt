package kr.heartpattern.spikotannotationprocessor

import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement

//@AutoService(Processor::class)
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
class SpikotAnnotationProcessor : AbstractProcessor() {
    val processors = listOf(
        VerifyingProcessor(),
        PlayerPropertyProcessor(),
        PlayerFlagPropertyProcessor()
    )

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        var result = true
        for (processor in processors) {
            result = result && processor.process(annotations, roundEnv)
        }
        return result
    }

    override fun init(processingEnv: ProcessingEnvironment?) {
        for (processor in processors) {
            processor.init(processingEnv)
        }
    }
}