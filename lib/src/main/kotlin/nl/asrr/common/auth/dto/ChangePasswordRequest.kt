package nl.asrr.common.auth.dto

import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

data class ChangePasswordRequest(

    @NotBlank
    @Email
    val email: String,

    val oldPassword: String,

    @NotBlank
    val newPassword: String
)
