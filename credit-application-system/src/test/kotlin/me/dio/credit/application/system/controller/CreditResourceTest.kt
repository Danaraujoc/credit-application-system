package me.dio.credit.application.system.controller

import com.fasterxml.jackson.databind.ObjectMapper
import me.dio.credit.application.system.dto.request.CreditDto
import me.dio.credit.application.system.dto.request.CustomerDto
import me.dio.credit.application.system.dto.response.CreditView
import me.dio.credit.application.system.entity.Customer
import me.dio.credit.application.system.enummeration.Status
import me.dio.credit.application.system.repository.CreditRepository
import me.dio.credit.application.system.repository.CustomerRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@ContextConfiguration
class CreditResourceTest {
    @Autowired
    private lateinit var creditRepository: CreditRepository

    @Autowired
    private lateinit var customerRepository: CustomerRepository

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    companion object {
        const val URL: String = "/api/credits"
    }

    @BeforeEach
    fun setup() {
        creditRepository.deleteAll()
        customerRepository.deleteAll()
    }

    @AfterEach
    fun tearDown() {
        creditRepository.deleteAll()
        customerRepository.deleteAll()
    }

    @Test
    fun `should save a credit and return 201 status`() {
        //deve salvar um crédito e retornar o status 201
        //Given
        val customerDto: CustomerDto = builderCustomerDto()
        val customer = customerRepository.save(customerDto.toEntity())

        val creditDto: CreditDto = buildCreditDto(customerId = customer.id)
        val valueAsString: String = objectMapper.writeValueAsString(creditDto)

        // when
        val result = mockMvc.perform(
            MockMvcRequestBuilders.post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(valueAsString)
        )

        // then
        val creditCode = creditRepository.findAll().first().creditCode
        val expectedMessage = "Credit $creditCode - Customer ${customer.email} saved!"

        result.andExpect(MockMvcResultMatchers.status().isCreated)
            .andExpect(MockMvcResultMatchers.content().string(expectedMessage))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should not save a credit with invalid customer id and return 400 status`() {
        //não deve salvar um crédito com ID de cliente inválido e devolver 400
        // given
        val invalidCustomerId = 999L
        val creditDto = buildCreditDto(customerId = invalidCustomerId)
        val valueAsString = objectMapper.writeValueAsString(creditDto)

        // when
        // then
        mockMvc.perform(
            MockMvcRequestBuilders.post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(valueAsString)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Bad Request ! Consult the documentation"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.exception")
                    .value("class me.dio.credit.application.system.exception.BusinessException")
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.details[*]").isNotEmpty)
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should find all credits by customer id and return 200 status`() {
        //deve encontrar todos os créditos por ID do cliente e retornar o status 200
        // given
        val customerDto: CustomerDto = builderCustomerDto()
        val customer = customerRepository.save(customerDto.toEntity())

        val creditDto: CreditDto = buildCreditDto(customerId = customer.id)
        val valueAsString: String = objectMapper.writeValueAsString(creditDto)
        mockMvc.perform(
            MockMvcRequestBuilders.post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(valueAsString)
        )

        // when
        // then
        mockMvc.perform(
            MockMvcRequestBuilders.get("$URL?customerId=${customer.id}")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$[0].creditValue").value(creditDto.creditValue.toDouble()))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$[0].numberOfInstallments").value(creditDto.numberOfInstallments)
            )
            .andDo(MockMvcResultHandlers.print())

    }

    @Test
    fun `find credit by credit code and customer id and return status 200`() {
        // Given
        val customerDto: CustomerDto = builderCustomerDto()
        val customer = customerRepository.save(customerDto.toEntity())

        val creditDto: CreditDto = buildCreditDto(customerId = customer.id)
        val valueAsString: String = objectMapper.writeValueAsString(creditDto)
        val resultSave = mockMvc.perform(
            MockMvcRequestBuilders.post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(valueAsString)
        )

        val creditCode = creditRepository.findAll().first().creditCode

        // When
        val result = mockMvc.perform(
            MockMvcRequestBuilders.get("$URL/$creditCode?customerId=${customer.id}")
                .accept(MediaType.APPLICATION_JSON)
        )

        // Then
        val expectedCreditView = buildCreditView(customer, creditCode)

        result.andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.creditCode").value(expectedCreditView.creditCode.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.creditValue").value(expectedCreditView.creditValue.toDouble()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.numberOfInstallment").value(expectedCreditView.numberOfInstallment))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(expectedCreditView.status.toString()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.emailCustomer").value(expectedCreditView.emailCustomer))
            .andExpect(MockMvcResultMatchers.jsonPath("$.incomeCustomer").value(expectedCreditView.incomeCustomer))
            .andDo(MockMvcResultHandlers.print())
    }

    @Test
    fun `should not get credit by invalid credit code and return 400 status`() {
        //não deve obter crédito por código de crédito inválido e retornar status 400
        // given
        val invalidCreditCode = UUID.randomUUID()
        val customerDto: CustomerDto = builderCustomerDto()
        val customer = customerRepository.save(customerDto.toEntity())
        // when
        // then
        mockMvc.perform(
            MockMvcRequestBuilders.get("$URL/$invalidCreditCode?customerId=${customer.id}")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.title").value("Bad Request ! Consult the documentation"))
            .andExpect(MockMvcResultMatchers.jsonPath("$.timestamp").exists())
            .andExpect(MockMvcResultMatchers.jsonPath("$.status").value(400))
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.exception")
                    .value("class me.dio.credit.application.system.exception.BusinessException")
            )
            .andExpect(MockMvcResultMatchers.jsonPath("$.details[*]").isNotEmpty)
            .andDo(MockMvcResultHandlers.print())
    }

    private fun buildCreditDto(customerId: Long?): CreditDto = CreditDto(
        creditValue = BigDecimal.valueOf(1000.0),
        dayFirstOfInstallment = LocalDate.now().plusMonths(3),
        numberOfInstallments = 12,
        customerId = customerId ?: 1L,

        )

    private fun builderCustomerDto(
        firstName: String = "Daniel",
        lastName: String = "Araujo",
        cpf: String = "28475934625",
        email: String = "daniel@email.com",
        income: BigDecimal = BigDecimal.valueOf(1000.0),
        password: String = "1234",
        zipCode: String = "000000",
        street: String = "Rua do Daniel, 123",
    ) = CustomerDto(
        firstName = firstName,
        lastName = lastName,
        cpf = cpf,
        email = email,
        income = income,
        password = password,
        zipCode = zipCode,
        street = street
    )

    fun buildCreditView(customer: Customer, creditCode: UUID = UUID.randomUUID()): CreditView {
        return CreditView(
            creditCode = creditCode,
            creditValue = BigDecimal.valueOf(1000),
            numberOfInstallment = 12,
            status = Status.IN_PROGRESS,
            emailCustomer = customer.email,
            incomeCustomer = customer.income
        )
    }
}
