package nl.asrr.common.auth.model

enum class Role {
    SUPER_ADMIN,
    _COMPANY_ADMIN,
    _PROJECT_ADMIN
}

inline fun <reified T : Enum<T>> enumContains(name: String): Boolean {
    return enumValues<T>().any { it.name == name }
}
