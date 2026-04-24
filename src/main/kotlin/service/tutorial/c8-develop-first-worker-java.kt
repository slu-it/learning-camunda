package service.tutorial

import io.camunda.client.annotation.JobWorker
import io.camunda.client.api.response.ActivatedJob
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class CreditCardService {

    private val log = LoggerFactory.getLogger(javaClass)

    fun chargeCreditCard(request: PaymentRequest): UUID {
        log.info("Charging Credit Card")
        log.info("$request")
        return UUID.randomUUID()
    }
}

@Component
class ChargeCreditCardWorker(
    private val service: CreditCardService
) {

    @JobWorker(type = "tutorial:chargeCreditCard:1")
    fun handle(job: ActivatedJob): Map<String, Any> {
        val request = PaymentRequest(
            reference = job.getVariable("reference") as String,
            amount = (job.getVariable("amount") as Number).toString().toBigDecimal(),
            card = PaymentRequest.Card(
                number = job.getVariable("cardNumber") as String,
                expiry = job.getVariable("cardExpiry") as String,
                cvc = job.getVariable("cardCVC") as String,
            )
        )
        val confirmation = service.chargeCreditCard(request)

        return mapOf("confirmation" to confirmation.toString())
    }
}

data class PaymentRequest(
    val reference: String,
    val amount: BigDecimal,
    val card: Card,
) {
    data class Card(
        val number: String,
        val expiry: String,
        val cvc: String,
    )
}
