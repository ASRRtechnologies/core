/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.testing.architecture

import nl.asrr.core.testing.architecture.ArchitectureScan.require
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry
import org.springframework.context.annotation.AnnotationBeanNameGenerator
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.stereotype.Component

/**
 * Two stereotype-annotated classes that resolve to the same Spring bean name fail the whole
 * application at startup with `ConflictingBeanDefinitionException`. Alpha v3.399.0 crashlooped on
 * dev because two packages each declared a `GitHubIntegrationController`. Without a full
 * context-load test that only surfaces when the deployed pod crashloops; this finds it in seconds.
 */
abstract class BeanNameUniquenessContract {

    abstract val basePackage: String

    @Test
    fun `component-scanned bean names are unique`() {
        val scanner = ClassPathScanningCandidateComponentProvider(false)
        scanner.addIncludeFilter(AnnotationTypeFilter(Component::class.java))
        val registry = SimpleBeanDefinitionRegistry()

        val byName = mutableMapOf<String, MutableList<String>>()
        scanner.findCandidateComponents(basePackage)
            .filterIsInstance<AnnotatedBeanDefinition>()
            .forEach { definition ->
                val beanName = AnnotationBeanNameGenerator.INSTANCE.generateBeanName(definition, registry)
                byName.getOrPut(beanName) { mutableListOf() }.add(definition.beanClassName ?: "?")
            }

        val conflicts = byName.filterValues { it.size > 1 }
        require(conflicts.isEmpty()) {
            "Conflicting Spring bean names (add an explicit name to the @Component/@Service/@RestController annotation):\n" +
                conflicts.entries.joinToString("\n") { (name, classes) -> "  '$name' -> ${classes.joinToString(", ")}" }
        }
    }
}
