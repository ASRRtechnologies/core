package nl.asrr.common.validation

class EmailValidator {

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", RegexOption.IGNORE_CASE)

        fun isValid(email: String): Boolean {
            return EMAIL_REGEX.matches(email)
        }
    }
}