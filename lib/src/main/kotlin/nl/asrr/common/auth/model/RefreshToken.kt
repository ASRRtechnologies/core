package nl.asrr.common.auth.model

import com.fasterxml.jackson.annotation.JsonFormat
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document("refreshToken")
data class RefreshToken(

    @Id
    val id: String,

    val username: String,

    val token: String,

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val expires: LocalDateTime
)
