package service.tutorial

import io.camunda.client.annotation.JobWorker
import io.camunda.client.annotation.Variable
import io.camunda.client.api.response.ActivatedJob
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.time.Duration

@Service
@Suppress("MagicNumber", "FunctionOnlyReturningConstant")
class TrackingOrderService {

    private val log = LoggerFactory.getLogger(javaClass)

    @Throws(InterruptedException::class)
    fun trackOrderStatus(job: ActivatedJob) {
        log(job)
        Thread.sleep(Duration.ofSeconds(5).toMillis())
    }

    @Throws(InterruptedException::class)
    fun packItems(job: ActivatedJob): Boolean {
        log(job)
        return true
    }

    @Throws(InterruptedException::class)
    fun processPayment(job: ActivatedJob): String {
        log(job)
        return System.currentTimeMillis().toString()
    }

    private fun log(job: ActivatedJob) {
        log.debug("job: {}", job)
    }
}

@Component
class OrderWorker(
    private val service: TrackingOrderService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @JobWorker(type = "tutorial::trackOrderStatus:1", timeout = 10_000, maxJobsActive = 1, fetchVariables = ["orderId"])
    fun handle(job: ActivatedJob) {
        val orderId = job.getVariable("orderId")

        log.info("Order: {} Tracking status", orderId)
        service.trackOrderStatus(job)
        log.info("Order: {} Status tracked successfully", orderId)

        // will contain only orderId, since fetchVariables was used
        log.info("List of variables from Zeebe: {}", job.variables)
    }
}

@Component
class PackItemsWorker(
    private val service: TrackingOrderService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @JobWorker(type = "tutorial::packItems:1")
    fun handle(job: ActivatedJob, @Variable("orderId") orderId: String): Map<String, Any> {
        log.info("Order: {} Packing items", orderId)
        val packedItems = service.packItems(job)
        log.info("Order: {} Items packed successfully", orderId)

        // will contain only orderId, since @variable was used
        log.info("List of variables from Zeebe: {}", job.variables)

        return mapOf("packaged" to packedItems)
    }
}

@Component
class ProcessPaymentWorker(
    private val service: TrackingOrderService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @JobWorker(type = "tutorial::processPayment:1")
    fun handle(job: ActivatedJob): Map<String, Any> {
        val orderId = job.getVariable("orderId")

        log.info("Order: {} Processing payment", orderId)
        val paymentConfirmation = service.processPayment(job)
        log.info("Order: {} Payment processed successfully", orderId)

        // will contain all, since @Variable was not used
        log.info("List of variables from Zeebe: {}", job.variables)

        return mapOf("paymentConfirmation" to paymentConfirmation)
    }
}
