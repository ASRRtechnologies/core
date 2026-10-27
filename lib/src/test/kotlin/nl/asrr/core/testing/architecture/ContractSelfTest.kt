/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.testing.architecture

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opentest4j.AssertionFailedError
import kotlin.test.assertContains

private const val GOOD = "nl.asrr.core.testing.sample"
private const val BAD = "nl.asrr.core.testing.samplebad"

/** The contracts pass on a clean package and name the offenders in a broken one. */
class ContractSelfTest {

    @Nested
    inner class GoodPackagePasses {
        @Nested
        inner class Tenant : TenantIsolationContract() {
            override val basePackage = GOOD
            override val baselineDir = "architecture/none"
            override val scanTestOutput = true
        }

        @Nested
        inner class Beans : BeanNameUniquenessContract() {
            override val basePackage = GOOD
        }

        @Nested
        inner class Locks : ScheduledJobLockContract() {
            override val basePackage = GOOD
            override val minScheduledMethods = 2
        }

        @Nested
        inner class Booleans : BooleanJsonPropertyContract() {
            override val basePackage = GOOD
            override val scanTestOutput = true
        }
    }

    private val tenant = object : TenantIsolationContract() {
        override val basePackage = BAD
        override val baselineDir = "architecture/none"
        override val scanTestOutput = true
    }

    @Test
    fun `a document without tenantId is reported`() {
        val error = assertThrows<AssertionFailedError> { tenant.`every document carries a tenantId or is a reviewed global collection`() }
        assertContains(error.message!!, "BadNote")
    }

    @Test
    fun `an unscoped repository method is reported`() {
        val error = assertThrows<AssertionFailedError> { tenant.`query methods on tenant-scoped repositories filter on tenantId`() }
        assertContains(error.message!!, "BadInvoiceRepository.findByNumber")
    }

    @Test
    fun `a tenant-blind repository call is reported with its caller`() {
        val error = assertThrows<AssertionFailedError> { tenant.`main code calls no tenant-blind method on a tenant-scoped repository`() }
        assertContains(error.message!!, "BadInvoiceService.find -> BadInvoiceRepository.findById")
    }

    @Test
    fun `a reviewed exception with a reason passes`() {
        val reviewed = object : TenantIsolationContract() {
            override val basePackage = BAD
            override val baselineDir = "architecture/samplebad"
            override val scanTestOutput = true
        }
        reviewed.`query methods on tenant-scoped repositories filter on tenantId`()
    }

    @Test
    fun `a duplicate bean name is reported`() {
        val error = assertThrows<AssertionFailedError> {
            object : BeanNameUniquenessContract() {
                override val basePackage = BAD
            }.`component-scanned bean names are unique`()
        }
        assertContains(error.message!!, "'badJobs'")
    }

    @Test
    fun `an unlocked scheduled job is reported`() {
        val error = assertThrows<AssertionFailedError> {
            object : ScheduledJobLockContract() {
                override val basePackage = BAD
            }.`every scheduled method is locked or explicitly runs on every instance`()
        }
        assertContains(error.message!!, "BadJobs.notifyOverdue")
    }

    @Test
    fun `an is-boolean without JsonProperty is reported`() {
        val error = assertThrows<AssertionFailedError> {
            object : BooleanJsonPropertyContract() {
                override val basePackage = BAD
                override val scanTestOutput = true
            }.`is-prefixed booleans keep their name in JSON`()
        }
        assertContains(error.message!!, "BadInvoice.isPaid")
    }
}
