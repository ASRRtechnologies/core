/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.testing.architecture

import com.fasterxml.jackson.annotation.JsonProperty
import org.junit.jupiter.api.Test
import org.springframework.asm.ClassReader
import java.lang.reflect.Modifier

/**
 * A Kotlin boolean named `isActive` compiles to a getter `isActive()`, which Jackson reads as the
 * property `active`: the API sends `active`, the frontend reads `isActive`, gets `undefined`, and
 * every toggle shows off. Each such property on a serialized type needs
 * `@field:JsonProperty("isActive")` (or `@get:`/`@param:`).
 *
 * Checks every class under [basePackage] that [isSerialized] (by default: `@Document` entities and
 * `*Dto`, `*Request`, `*Response` types). Adding the annotation to an existing property changes its
 * wire name, so an app that already ships such properties lists them in
 * `src/test/resources/<baselineFile>` (`Class.field  # why`); the list may only shrink.
 */
abstract class BooleanJsonPropertyContract {

    abstract val basePackage: String

    /** Also scan compiled test classes; only for testing the contract itself. */
    protected open val scanTestOutput: Boolean = false

    open val baselineFile: String = "architecture/is-boolean-json-names.txt"

    /** Which classes go over the wire. */
    open fun isSerialized(type: Class<*>): Boolean =
        type.isAnnotationPresent(org.springframework.data.mongodb.core.mapping.Document::class.java) ||
            listOf("Dto", "DTO", "Request", "Response").any { type.simpleName.endsWith(it) }

    private val IS_PREFIXED = Regex("^is[A-Z].*")

    protected fun offenders(): List<String> = ArchitectureScan.mainClassFiles(basePackage, scanTestOutput)
        .mapNotNull { resource ->
            val name = ClassReader(resource.inputStream.use { it.readBytes() }).className.replace('/', '.')
            runCatching { Class.forName(name, false, javaClass.classLoader) }.getOrNull()
        }
        .filter { !it.isSynthetic && !it.isAnonymousClass && isSerialized(it) }
        .flatMap { type ->
            type.declaredFields
                .filter { !Modifier.isStatic(it.modifiers) && (it.type == Boolean::class.javaPrimitiveType || it.type == java.lang.Boolean::class.java) }
                .filter { IS_PREFIXED.matches(it.name) }
                .filter { field ->
                    val annotated = field.getAnnotation(JsonProperty::class.java)?.value == field.name ||
                        runCatching { type.getMethod(field.name).getAnnotation(JsonProperty::class.java)?.value == field.name }.getOrDefault(false)
                    !annotated
                }
                .map { "${type.name}.${it.name}" }
        }
        .sorted()

    @Test
    fun `is-prefixed booleans keep their name in JSON`() {
        ArchitectureScan.ratchet(
            baselineFile,
            offenders().toSet(),
            "Kotlin `is`-booleans that Jackson serializes without the `is` (isActive -> active).",
            "Add @field:JsonProperty(\"<name>\"), or, for a property whose wire name clients already read, add it to the baseline",
        )
    }
}
