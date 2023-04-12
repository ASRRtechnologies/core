package nl.asrr.core.auth.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank

data class LoginRequest(

    @field:NotBlank
    @JsonProperty("username")
    val username: String,

    @field:NotBlank
    @JsonProperty("password")
    val password: String
)
