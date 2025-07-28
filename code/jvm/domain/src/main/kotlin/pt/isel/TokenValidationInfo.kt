package pt.isel

data class TokenValidationInfo(
    val validationInfo: String,
) {
    fun isValid(): Boolean = validationInfo.isNotBlank()
}
