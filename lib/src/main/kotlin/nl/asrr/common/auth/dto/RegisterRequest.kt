package nl.asrr.common.auth.dto

import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.constraints.NotBlank

data class RegisterRequest(

    @field:NotBlank
    @JsonProperty("username")
    val username: String,

    @field:NotBlank
    @JsonProperty("password")
    val password: String
)
