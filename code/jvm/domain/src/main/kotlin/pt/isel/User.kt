package pt.isel

data class User(
    val id: Int,
    val username: String,
    val passwordValidation: PasswordValidationInfo,
) {
    fun checkUsername(pass: String) = pass == this.passwordValidation.validationInfo
}
