package nl.asrr.common.auth.dto

import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.constraints.NotBlank

data class LoginRequest(

    @NotBlank
    @JsonProperty("username")
    val username: String,

    @NotBlank
    @JsonProperty("password")
    val password: String
)
