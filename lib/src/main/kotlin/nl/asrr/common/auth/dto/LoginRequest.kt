package nl.asrr.common.auth.dto

import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

data class LoginRequest(

    @NotBlank
    @Email
    @JsonProperty("username")
    val username: String,

    @NotBlank
    @JsonProperty("password")
    val password: String
)
