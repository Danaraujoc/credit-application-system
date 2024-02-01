package me.dio.credit.application.system.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import me.dio.credit.application.system.entity.Address
import me.dio.credit.application.system.entity.Customer
import org.hibernate.validator.constraints.br.CPF
import java.math.BigDecimal

data class CustomerDto(
    @field:NotEmpty(message = "Entrada invalida") val firstName: String,
    @field:NotEmpty(message = "Entrada invalida") val lastName: String,
    @field:CPF(message = "CPF invalido") val cpf: String,
    @field:NotNull(message = "Entrada invalida") val income: BigDecimal,
    @field:Email(message = "Email invalido") val email: String,
    @field:NotEmpty(message = "Entrada invalida") val password: String,
    @field:NotEmpty(message = "Entrada invalida") val zipCode: String,
    @field:NotEmpty(message = "Entrada invalida") val street: String

) {

    fun toEntity(): Customer = Customer(
        firstName = this.firstName,
        lastName = this.lastName,
        cpf = this.cpf,
        income = this.income,
        email = this.email,
        password = this.password,
        address = Address(
            zipCode = this.zipCode,
            street = this.street
        )

    )
}
