import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import me.dio.credit.application.system.entity.Address
import me.dio.credit.application.system.entity.Credit
import me.dio.credit.application.system.entity.Customer
import me.dio.credit.application.system.enummeration.Status
import me.dio.credit.application.system.exception.BusinessException
import me.dio.credit.application.system.repository.CreditRepository
import me.dio.credit.application.system.service.impl.CreditService
import me.dio.credit.application.system.service.impl.CustomerService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.dao.DataIntegrityViolationException
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@ExtendWith(MockKExtension::class)
class CreditServiceTest {

    @MockK
    lateinit var creditRepository: CreditRepository

    @MockK
    lateinit var customerService: CustomerService

    @InjectMockKs
    lateinit var creditService: CreditService

    @Test
    fun `should save credit`() {
        //deve salvar o crédito.
        // Given
        val fakeCustomer: Customer = buildCustomer()
        val fakeCredit: Credit = buildCredit(customer = fakeCustomer)

        every { creditRepository.save(any()) } returns fakeCredit
        every { customerService.findById(any()) } returns (fakeCredit.customer ?: buildCustomer())

        // When
        var actual: Credit? = null
        var exception: BusinessException? = null

        try {
            actual = creditService.save(fakeCredit)
        } catch (e: BusinessException) {
            exception = e
        }

        // Then
        assertThat(exception).isNull()
        assertThat(actual).isNotNull
        assertThat(actual).isEqualTo(fakeCredit)

        // When
        /*val actual: Credit = creditService.save(fakeCredit)

        // Then
        assertThat(actual).isNotNull
        assertThat(actual).isEqualTo(fakeCredit)*/
    }

    @Test
    fun `should find all credits by customer`() {
        //deve encontrar todos os créditos pelo cliente.
        // Given
        val fakeCustomerId: Long = 1L
        val fakeCredits: List<Credit> = listOf(
            buildFakeCredit(customer = Customer(id = fakeCustomerId)),
            buildFakeCredit(customer = Customer(id = fakeCustomerId)),
            buildFakeCredit(customer = Customer(id = fakeCustomerId))
        )
        every { creditRepository.findAllByCustomerId(fakeCustomerId) } returns fakeCredits

        // When
        val actual: List<Credit> = creditService.findAllByCustomer(fakeCustomerId)

        // Then
        assertThat(actual).isNotNull
        assertThat(actual).hasSize(fakeCredits.size)
    }

    @Test
    fun `should find credit by credit code for the correct customer`() {
        //deve encontrar o crédito pelo código do crédito para o cliente correto
        // Given
        val fakeCustomerId: Long = 1L
        val fakeCreditCode: UUID = UUID.randomUUID()
        val fakeCredit: Credit = buildFakeCredit(customer = Customer(id = fakeCustomerId))
        every { creditRepository.findByCreditCode(fakeCreditCode) } returns fakeCredit

        // When
        val actual: Credit = creditService.findByCreditCode(fakeCustomerId, fakeCreditCode)

        // Then
        assertThat(actual).isNotNull
        assertThat(actual).isEqualTo(fakeCredit)
    }

    @Test
    fun `should not find credit by credit code for the incorrect customer and throw BusinessException`() {
        //não deve encontrar crédito pelo código do crédito para o cliente incorreto e deve lançar uma BusinessException.
        // Given
        val fakeCustomerId: Long = 1L
        val fakeCreditCode: UUID = UUID.randomUUID()
        val fakeCredit: Credit = buildFakeCredit(customer = Customer(id = fakeCustomerId + 1))
        every { creditRepository.findByCreditCode(fakeCreditCode) } returns fakeCredit

        // When
        // Then
        assertThatExceptionOfType(BusinessException::class.java)
            .isThrownBy { creditService.findByCreditCode(fakeCustomerId, fakeCreditCode) }
            .withMessage("Credit code $fakeCreditCode not found for customer $fakeCustomerId")
    }

    @Test
    fun `should throw BusinessException when trying to save credit with invalid first installment date`() {
        //deve lançar uma BusinessException ao tentar salvar crédito com data da primeira parcela inválida.
        // Given
        val fakeCredit: Credit = buildFakeCredit(dayFirstInstallment = LocalDate.now().minusMonths(1))

        every { creditRepository.save(any()) } throws DataIntegrityViolationException("Invalid Date")

        // When
        // Then
        assertThatExceptionOfType(BusinessException::class.java)
            .isThrownBy { creditService.save(fakeCredit) }
            .withMessage("Invalid Date")
    }


    private fun buildFakeCredit(
        creditValue: BigDecimal = BigDecimal("1000.00"),
        dayFirstInstallment: LocalDate = LocalDate.now().plusDays(7),
        numberOfInstallments: Int = 12,
        status: Status = Status.IN_PROGRESS,
        customer: Customer? = null,
        id: Long? = null
    ): Credit {
        return Credit(
            creditValue = creditValue,
            dayFirstInstallment = dayFirstInstallment,
            numberOfInstallments = numberOfInstallments,
            status = status,
            customer = customer,
            id = id
        )
    }

    private fun buildCustomer(
        firstName: String = "Daniel",
        lastName: String = "Araujo",
        cpf: String = "28475934625",
        email: String = "daniel@gmail.com",
        password: String = "12345",
        zipCode: String = "12345",
        street: String = "Rua dos Bobos",
        income: BigDecimal = BigDecimal.valueOf(1000.0),
        id: Long = 1L
    ) = Customer(
        firstName = firstName,
        lastName = lastName,
        cpf = cpf,
        email = email,
        password = password,
        address = Address(
            zipCode = zipCode,
            street = street,
        ),
        income = income,
        id = id
    )

    private fun buildCredit(
        creditValue: BigDecimal = BigDecimal.valueOf(500.0),
        dayFirstInstallment: LocalDate = LocalDate.of(2024, 5, 23),
        numberOfInstallments: Int = 5,
        customer: Customer
    ): Credit = Credit(
        creditValue = creditValue,
        dayFirstInstallment = dayFirstInstallment,
        numberOfInstallments = numberOfInstallments,
        customer = customer
    )

}
