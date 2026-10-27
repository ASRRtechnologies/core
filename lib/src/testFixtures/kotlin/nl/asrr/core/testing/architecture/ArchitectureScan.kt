/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.testing.architecture

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.type.filter.TypeFilter

/** Classpath scanning shared by the architecture contracts. */
object ArchitectureScan {

    /** Classes under [basePackage] matching any of [filters], abstract classes and interfaces included. */
    fun classes(basePackage: String, vararg filters: TypeFilter): List<Class<*>> {
        val provider = object : ClassPathScanningCandidateComponentProvider(false) {
            override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition): Boolean = true
        }
        filters.forEach { provider.addIncludeFilter(it) }
        return provider.findCandidateComponents(basePackage)
            .mapNotNull { it.beanClassName }
            .map { Class.forName(it, false, ArchitectureScan::class.java.classLoader) }
            .sortedBy { it.name }
    }

    /** The compiled main classes under [basePackage]; test output is left out unless [includeTests]. */
    fun mainClassFiles(basePackage: String, includeTests: Boolean = false): List<Resource> =
        PathMatchingResourcePatternResolver().getResources("classpath*:${basePackage.replace('.', '/')}/**/*.class")
            .filter { resource -> includeTests || resource.url.toString().let { !it.contains("/test/") && !it.contains("/test-") && !it.contains("testFixtures") } }

    /**
     * A reviewed-exceptions file on the test classpath: one entry per line, `#` starts a comment
     * (the reason). Null when the file does not exist.
     */
    fun baseline(resourcePath: String): Set<String>? {
        val text = ArchitectureScan::class.java.classLoader.getResourceAsStream(resourcePath)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: return null
        return text.lines().map { it.substringBefore('#').trim() }.filter { it.isNotEmpty() }.toSet()
    }

    /**
     * Fails when [actual] differs from the baseline in [resourcePath]: a new entry is a finding to
     * fix (or to add with a reason), a stale one must be deleted. A baseline may only shrink; never
     * regenerate one to get green, that hides exactly what the contract exists for.
     */
    fun ratchet(resourcePath: String, actual: Set<String>, whatIsWrong: String, howToFix: String) {
        val allowed = baseline(resourcePath) ?: emptySet()
        val added = actual - allowed
        val stale = allowed - actual
        require(added.isEmpty() && stale.isEmpty()) {
            buildString {
                if (added.isNotEmpty()) {
                    append(whatIsWrong).append("\n").append(howToFix).append(" (src/test/resources/").append(resourcePath).append(", `entry  # reason`):\n  ")
                    append(added.sorted().joinToString("\n  ")).append("\n")
                }
                if (stale.isNotEmpty()) {
                    append("Entries in src/test/resources/").append(resourcePath).append(" that no longer match any code. The baseline may only shrink: delete these lines.\n  ")
                    append(stale.sorted().joinToString("\n  "))
                }
            }
        }
    }

    /** Like Kotlin's `assert`, but independent of the JVM's `-ea` flag. */
    inline fun require(condition: Boolean, message: () -> String) {
        if (!condition) org.junit.jupiter.api.Assertions.fail<Unit>(message())
    }
}
