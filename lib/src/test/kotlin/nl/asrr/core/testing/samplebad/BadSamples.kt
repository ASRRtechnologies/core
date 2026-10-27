/* Copyright 2017-2026 ASRR B.V. */
package nl.asrr.core.testing.samplebad

import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Document("bad_note")
data class BadNote(val id: String, val text: String)

@Document("bad_invoice")
data class BadInvoice(val id: String, val tenantId: String, val isPaid: Boolean = false)

interface BadInvoiceRepository : MongoRepository<BadInvoice, String> {
    fun findByNumber(number: String): BadInvoice?
}

class BadInvoiceService(private val repository: BadInvoiceRepository) {
    fun find(id: String) = repository.findById(id)
}

@Component
class BadJobs {
    @Scheduled(fixedDelay = 60_000)
    fun notifyOverdue() = Unit
}

/** A second bean called `badJobs`. */
@Component("badJobs")
class OtherBadJobs
