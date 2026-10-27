/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.testing.architecture

import nl.asrr.core.generics.model.ITenantCrudEntity
import nl.asrr.core.testing.architecture.ArchitectureScan.require
import org.junit.jupiter.api.Test
import org.springframework.asm.ClassReader
import org.springframework.asm.ClassVisitor
import org.springframework.asm.MethodVisitor
import org.springframework.asm.Opcodes
import org.springframework.core.ResolvableType
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.core.type.filter.AssignableTypeFilter
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.Aggregation
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.repository.Repository
import java.lang.reflect.Method

/**
 * Every tenant's data lives in the same collections, separated only by a `tenantId` in each query.
 * A query that forgets it is a cross-tenant leak, and nothing fails. This contract checks the whole
 * data layer by classpath and bytecode scan, so a new collection, repository method or repository
 * call is covered the moment it is compiled:
 *
 * 1. every `@Document` carries a `tenantId`, or is a reviewed global collection ([globalDocuments]);
 * 2. every query method declared on a repository of a tenant-scoped document filters on `tenantId`
 *    (a derived name with `TenantId`, or an `@Query` / `@Aggregation` that mentions `tenantId`);
 * 3. main code calls no tenant-blind method on such a repository: the inherited `findById`,
 *    `findAll`, `findAllById`, `existsById`, `deleteById`, `deleteAll()`, `count()`, `findOneById`,
 *    or a declared method from (2) that has no `tenantId`.
 *
 * In Alpha a single copied `findById` turned dev red three times in one month before this ran on
 * every push. Use it with a subclass in the app's test tree:
 *
 * ```
 * class TenantIsolationTest : TenantIsolationContract() {
 *     override val basePackage = "nl.asrr.myapp"
 *     override val globalDocuments = mapOf("Tenant" to "the tenant registry itself")
 * }
 * ```
 *
 * (2) and (3) are ratchets on `src/test/resources/<baselineDir>/unscoped-repository-methods.txt` and
 * `unscoped-repository-calls.txt`: an entry is a reviewed exception with its reason
 * (`entry  # reason`), and the files may only shrink. Start an existing app with a baseline of what
 * is there; a new app should not need one.
 */
abstract class TenantIsolationContract {

    /** The application's root package, e.g. `nl.asrr.alpha`. */
    abstract val basePackage: String

    /**
     * `@Document` classes without a `tenantId`, reviewed as global (platform-level, or looked up by
     * an unguessable token before any tenant is known). Key = simple class name, value = the reason.
     * Do not add a tenant's business data here: give the document a `tenantId` instead.
     */
    open val globalDocuments: Map<String, String> = emptyMap()

    /** Test resource directory of the two baselines. */
    open val baselineDir: String = "architecture/tenant-isolation"

    /** Lower bounds that prove the scan found the code; a broken scan would pass every check vacuously. */
    open val minDocuments: Int = 1
    open val minRepositories: Int = 1
    open val minClassFiles: Int = 1

    /** Also scan compiled test classes; only for testing the contract itself. */
    protected open val scanTestOutput: Boolean = false

    private fun hasTenantId(type: Class<*>): Boolean {
        if (ITenantCrudEntity::class.java.isAssignableFrom(type)) return true
        var current: Class<*>? = type
        while (current != null && current != Any::class.java) {
            if (current.declaredFields.any { it.name == "tenantId" }) return true
            if (current.declaredMethods.any { it.name == "getTenantId" && it.parameterCount == 0 }) return true
            current = current.superclass
        }
        return false
    }

    /** Repository interfaces of the application whose domain type is tenant-scoped. */
    private val tenantRepositories: List<Class<*>> by lazy {
        val repositories = ArchitectureScan.classes(basePackage, AssignableTypeFilter(Repository::class.java)).filter { it.isInterface }
        require(repositories.size >= minRepositories) { "expected at least $minRepositories repository interfaces under $basePackage, found ${repositories.size}" }
        repositories.filter { repository ->
            val domain = ResolvableType.forClass(repository).`as`(Repository::class.java).getGeneric(0).resolve()
            domain != null && hasTenantId(domain)
        }
    }

    private fun isScoped(method: Method): Boolean {
        method.getAnnotation(Query::class.java)?.let { return (it.value + it.fields + it.sort).contains("tenantId") }
        method.getAnnotation(Aggregation::class.java)?.let { return (it.pipeline.joinToString() + it.value.joinToString()).contains("tenantId") }
        return method.name.contains("TenantId")
    }

    /** Query methods declared by a tenant repository itself that do not filter on `tenantId`, as `Repository.method`. */
    protected fun unscopedDeclaredMethods(): Set<String> = tenantRepositories.flatMap { repository ->
        repository.declaredMethods
            .filter { !it.isSynthetic && !it.isBridge && !it.isDefault }
            .filter { !isScoped(it) }
            .map { "${repository.simpleName}.${it.name}" }
    }.toSortedSet()

    /** Inherited CrudRepository / MongoRepository / ICrudRepository methods that ignore the tenant. */
    private val tenantBlindInherited = setOf(
        "findById", "findOneById", "findAll", "findAllById", "existsById", "deleteById", "deleteAllById", "findAllBy",
    )

    /** `deleteAll()` and `count()` without arguments wipe or count every tenant; with an argument they act on given entities. */
    private val tenantBlindWithoutArguments = setOf("deleteAll", "count")

    /**
     * Stable name of the code that makes a call: `OuterClass.method`, with Kotlin's lambda and
     * default-argument suffixes folded into the enclosing method so the key survives edits.
     */
    private fun callerKey(internalClassName: String, methodName: String): String {
        val simple = internalClassName.substringAfterLast('/')
        val outer = simple.substringBefore('$')
        val inner = simple.split('$').drop(1).firstOrNull { it.isNotEmpty() && !it.all(Char::isDigit) && it != "Companion" }
        val method = methodName.substringBefore('$')
        val name = if (inner != null && method in setOf("invoke", "invokeSuspend", "<init>", "apply", "accept", "get", "run", "call")) inner else method
        return "$outer.$name"
    }

    /** Every call to a tenant-blind method on a tenant repository from main code, as `Caller.method -> Repository.method`. */
    protected fun unscopedCalls(): Set<String> {
        val unscopedDeclared = unscopedDeclaredMethods()
        val repositoryNames = tenantRepositories.associateBy { it.name.replace('.', '/') }
        val classFiles = ArchitectureScan.mainClassFiles(basePackage, scanTestOutput)
        require(classFiles.size >= minClassFiles) { "expected at least $minClassFiles main classes under $basePackage, found ${classFiles.size}" }

        val calls = sortedSetOf<String>()
        classFiles.forEach { resource ->
            val reader = ClassReader(resource.inputStream.use { it.readBytes() })
            reader.accept(
                object : ClassVisitor(Opcodes.ASM9) {
                    override fun visitMethod(access: Int, name: String, descriptor: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor {
                        val caller = callerKey(reader.className, name)
                        return object : MethodVisitor(Opcodes.ASM9) {
                            override fun visitMethodInsn(opcode: Int, owner: String, method: String, descriptor: String, isInterface: Boolean) {
                                val repository = repositoryNames[owner] ?: return
                                val blind = method in tenantBlindInherited ||
                                    (method in tenantBlindWithoutArguments && descriptor.startsWith("()")) ||
                                    "${repository.simpleName}.$method" in unscopedDeclared
                                if (blind) calls.add("$caller -> ${repository.simpleName}.$method")
                            }
                        }
                    }
                },
                ClassReader.SKIP_FRAMES or ClassReader.SKIP_DEBUG,
            )
        }
        return calls
    }

    @Test
    fun `every document carries a tenantId or is a reviewed global collection`() {
        val documents = ArchitectureScan.classes(basePackage, AnnotationTypeFilter(Document::class.java))
        require(documents.size >= minDocuments) { "expected at least $minDocuments @Document classes under $basePackage, found ${documents.size}" }

        val global = documents.filter { !hasTenantId(it) }
        val unreviewed = global.filter { it.simpleName !in globalDocuments }.map { it.name }
        val stale = globalDocuments.keys.filter { name -> global.none { it.simpleName == name } }

        require(unreviewed.isEmpty() && stale.isEmpty()) {
            buildString {
                if (unreviewed.isNotEmpty()) {
                    append("@Document classes without a `tenantId`. Every tenant shares the collection, so nothing keeps one ")
                    append("tenant's records from another's. Add `val tenantId: String` (implement ITenantCrudEntity for the generic ")
                    append("CRUD layer), or, if the collection is genuinely platform-wide, add it to globalDocuments with a reason:\n  ")
                    append(unreviewed.joinToString("\n  ")).append("\n")
                }
                if (stale.isNotEmpty()) append("globalDocuments entries that are gone or now carry a tenantId: ${stale.joinToString()}")
            }
        }
    }

    @Test
    fun `query methods on tenant-scoped repositories filter on tenantId`() {
        ArchitectureScan.ratchet(
            "$baselineDir/unscoped-repository-methods.txt",
            unscopedDeclaredMethods(),
            "Repository methods on a tenant-scoped document that do not filter on tenantId. Called with a user-supplied " +
                "value they return or change another tenant's records.",
            "Add the tenant to the query (findByTenantIdAndX(tenantId, x), or `'tenantId': ?0` in the @Query). If the " +
                "lookup is tenant-blind by design (a hashed public token, a scheduler sweeping all tenants), add the line to the baseline",
        )
    }

    @Test
    fun `main code calls no tenant-blind method on a tenant-scoped repository`() {
        ArchitectureScan.ratchet(
            "$baselineDir/unscoped-repository-calls.txt",
            unscopedCalls(),
            "Calls to a tenant-blind repository method (findById, findAll, existsById, deleteById, findAllById, " +
                "findOneById, deleteAll(), count(), or an unscoped declared query) on a tenant-scoped repository. With an " +
                "id from a request, this reads or deletes another tenant's record.",
            "Use the tenant-scoped variant (findOneByIdAndTenantId(id, securityService.getTenantId()), " +
                "findAllByIdInAndTenantId, existsByTenantIdAndId, deleteByIdAndTenantId) or the ITenantCrudService methods. " +
                "If the call is tenant-blind by design (a scheduler over all tenants, a public token lookup, a record re-read by " +
                "an id that came from a scoped query), add the line to the baseline",
        )
    }
}
