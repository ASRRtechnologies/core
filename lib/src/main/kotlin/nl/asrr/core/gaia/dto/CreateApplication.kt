package nl.asrr.core.gaia.dto

data class CreateApplication(
    val name: String,
    val project: String,
    val company: String,
    val profile: String
)