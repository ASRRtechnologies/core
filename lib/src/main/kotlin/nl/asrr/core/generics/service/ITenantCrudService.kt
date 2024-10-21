/* Copyright 2017-2022 ASRR B.V. */
package nl.asrr.core.generics.service

import nl.asrr.core.auth.service.ISecurityService
import nl.asrr.core.exceptions.NotFoundException
import nl.asrr.core.generics.model.ITenantCrudEntity
import nl.asrr.core.generics.repository.ITenantCrudRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.TextCriteria

abstract class ITenantCrudService<T : ITenantCrudEntity>(
    override val repository: ITenantCrudRepository<T>,
    override val mongoTemplate: MongoTemplate,
    override val securityService: ISecurityService
) : ICrudService<T>(repository, mongoTemplate, securityService) {

    override fun find(id: String): T {
        return repository.findOneByIdAndTenantId(id, securityService.getTenantId()) ?: throw NotFoundException(
            "${this::class.simpleName} with id $id not found"
        )
    }

    override fun findList(ids: List<String>): List<T> {
        return repository.findAllByIdInAndTenantId(ids, securityService.getTenantId())
    }

    override fun find(pageable: Pageable, search: String): Page<T> {
        val tenantId = securityService.getTenantId()

        if (search.isBlank()) {
            return repository.findAllByTenantId(tenantId, pageable)
        }

        val criteria = TextCriteria().matchingAny(search)

        return repository.findAllByTenantId(tenantId, criteria, pageable)
    }

    override fun findAll(): List<T> {
        return repository.findAllByTenantId(securityService.getTenantId())
    }

    override fun exists(id: String, strict: Boolean): Boolean {
        val exists = repository.existsByTenantIdAndId(securityService.getTenantId(), id)
        if (strict && !exists) throw NotFoundException(id)
        return exists
    }

    override fun delete(id: String) {
        repository.deleteByIdAndTenantId(id, securityService.getTenantId())
    }

    override fun delete(ids: List<String>) {
        repository.deleteAllByIdAndTenantId(ids, securityService.getTenantId())
    }

}
