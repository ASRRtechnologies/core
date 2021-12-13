package nl.asrr.common.auth.dto

import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

data class RegisterRequest(

    @NotBlank
    @Email
    val email: String,

    @NotBlank
    val password: String,

    @NotBlank
    val fullName: String
)
