package nl.asrr.core.auth.service

interface ISecurityService {
    fun getTenantId(): String
    fun getUserId(): String
}