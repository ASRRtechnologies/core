/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.testing.architecture

import org.junit.jupiter.api.Test
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.SecurityAnnotationScanners
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Every endpoint needs a `@PreAuthorize`, and `isAuthenticated()` is not "staff only": a customer
 * portal user, a subcontractor and a pending invite all sign in with an ordinary JWT. In September
 * 2026 an Alpha buyer could read the tenant's roadmap, every support ticket and the branches with
 * their IBAN, all behind `isAuthenticated()`.
 *
 * So every endpoint guarded by nothing more than `isAuthenticated()`, or by nothing at all outside
 * the anonymous [publicPathPrefixes], must be listed in `src/test/resources/<reviewedFile>` once
 * someone has checked it is safe for any signed-in user (the caller's own data, harmless reference
 * data, or something the service scopes itself), as `Controller.method  # why`. A new one fails the
 * build until that call is made; usually the answer is a role or a permission, not an entry.
 */
abstract class EndpointAuthorizationContract {

    abstract val basePackage: String

    /** Path prefixes or fragments that are anonymous by design, e.g. `/public/`. */
    open val publicPathPrefixes: List<String> = listOf("/public/", "/public")

    open val reviewedFile: String = "architecture/endpoints-open-to-any-user.txt"

    private val scanner = SecurityAnnotationScanners.requireUnique(PreAuthorize::class.java)

    private fun controllers(): List<Class<*>> =
        ArchitectureScan.classes(basePackage, AnnotationTypeFilter(RestController::class.java)).filter { !Modifier.isAbstract(it.modifiers) }

    private fun endpoints(controller: Class<*>): List<Method> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<Method>()
        var current: Class<*>? = controller
        while (current != null && current != Any::class.java) {
            for (method in current.declaredMethods) {
                if (!Modifier.isPublic(method.modifiers) || method.isBridge || method.isSynthetic) continue
                val signature = method.name + method.parameterTypes.joinToString(",") { it.name }
                if (!seen.add(signature)) continue
                if (AnnotatedElementUtils.hasAnnotation(method, RequestMapping::class.java)) result.add(method)
            }
            current = current.superclass
        }
        return result
    }

    private fun paths(controller: Class<*>, method: Method): List<String> {
        val classPaths = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping::class.java)?.path?.toList().orEmpty().ifEmpty { listOf("") }
        val methodPaths = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping::class.java)?.path?.toList().orEmpty().ifEmpty { listOf("") }
        return classPaths.flatMap { c -> methodPaths.map { m -> "$c$m" } }
    }

    /** Endpoints open to any signed-in user (or to anyone), as `Controller.method`. */
    protected fun openEndpoints(): Set<String> = controllers().flatMap { controller ->
        endpoints(controller).mapNotNull { method ->
            val guard = scanner.scan(method, controller)?.value?.trim()
            if (guard != null && guard != "isAuthenticated()") return@mapNotNull null
            if (guard == null && paths(controller, method).all { path -> publicPathPrefixes.any { path.contains(it) || path.startsWith(it) } }) return@mapNotNull null
            "${controller.simpleName}.${method.name}"
        }
    }.toSortedSet()

    @Test
    fun `every endpoint open to any signed-in user has been reviewed`() {
        ArchitectureScan.ratchet(
            reviewedFile,
            openEndpoints(),
            "Endpoints without a @PreAuthorize, or guarded only by isAuthenticated(), that nobody reviewed. Every signed-in " +
                "user reaches them, customers and portal users included.",
            "Guard them with a role or permission, or, once checked safe for any signed-in user, add them to the reviewed list",
        )
    }
}
