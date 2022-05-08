package nl.asrr.common.gaia.dto

data class NodeUpdate(
        val company: String,
        val project: String,
        val applicationName: String,
        val id: String,
        val machineName: String,
        val user: String? = null,
        val os: String? = null,
        val usedRam: Long? = null,
        val totalRam: Long? = null,
        val profile: String? = null
)
