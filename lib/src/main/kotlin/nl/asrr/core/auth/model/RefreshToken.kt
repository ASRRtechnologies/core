package nl.asrr.core.auth.model

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
    val expires: LocalDateTime,

    /**
     * Set when this token has been rotated: points at the replacement token.
     * Within the rotation grace window the old token may be presented again
     * (client lost the rotation response) and receives the same replacement.
     */
    val replacedByToken: String? = null
)
