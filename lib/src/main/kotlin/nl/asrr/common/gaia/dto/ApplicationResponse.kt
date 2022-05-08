package nl.asrr.common.gaia.dto

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

data class ApplicationResponse(
        val id: String,
        var company: String,
        val project: String,
        val name: String,
        val updated: LocalDateTime,
        val nodes: MutableList<Node> = mutableListOf()
)

data class Node(
    val id: String,
    var machineName: String,
    var lastUpdated: LocalDateTime,
    var user: String? = null,
    var ip: String? = null,
    var os: String? = null,
    var usedRam: Long? = null,
    var totalRam: Long? = null,
    var profile: String? = null
)