package me.dio.credit.application.system.dto.request

import jakarta.validation.constraints.Future
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import me.dio.credit.application.system.entity.Credit
import me.dio.credit.application.system.entity.Customer
import java.math.BigDecimal
import java.time.LocalDate


data class CreditDto(
    @field:NotNull(message = "Entrada Invalida!") val creditValue: BigDecimal,
    @field:Future(message = "Deve ser uma data futuro") val dayFirstOfInstallment: LocalDate,
    @field:Min(value = 1) @field:Max(value = 48) val numberOfInstallments: Int,
    @field:NotNull(message = "Entrada Invalida") val customerId: Long
) {

    fun toEntity(): Credit = Credit(
        creditValue = this.creditValue,
        dayFirstInstallment = this.dayFirstOfInstallment,
        numberOfInstallments = this.numberOfInstallments,
        customer = Customer(id = this.customerId)
    )
}
