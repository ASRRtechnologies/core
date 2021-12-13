package nl.asrr.common.auth.dto

import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

data class ChangePasswordRequest(

    @NotBlank
    @Email
    @JsonProperty("email")
    val email: String,

    @JsonProperty("oldPassword")
    val oldPassword: String,

    @NotBlank
    @JsonProperty("newPassword")
    val newPassword: String
)
