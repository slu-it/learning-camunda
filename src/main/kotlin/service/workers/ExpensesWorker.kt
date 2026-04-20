package service.workers

import io.camunda.client.annotation.JobWorker
import io.camunda.client.annotation.Variable
import io.camunda.client.api.response.ActivatedJob
import io.camunda.client.exception.BpmnError
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.Currency
import kotlin.random.Random

@Component
class ExpensesWorker(
    @Value($$"${testing.errors.enabled:false}") private val errorsEnabled: Boolean,
) {

    private val log = getLogger(javaClass)

    @JobWorker(type = "expenses::process")
    fun process(
        job: ActivatedJob,
        @Variable("employeeId") employeeId: String,
        @Variable("amount") amount: BigDecimal,
        @Variable("currency") currency: Currency,
    ) {
        maybeThrowAnError()
        log.info("Processed $amount $currency expenses for $employeeId -- ${job.processInstanceKey}")
    }

    @Suppress("MagicNumber")
    private fun maybeThrowAnError() {
        if (errorsEnabled && Random.nextInt(100) <= 33) {
            throw BpmnError("E101", "oops")
        }
    }

    @JobWorker(type = "expenses::informEmployee")
    fun informEmployee(
        job: ActivatedJob,
        @Variable("employeeId") employeeId: String,
        @Variable("decision") decision: String,
    ) {
        log.info("Informed $employeeId about the decision '$decision' -- ${job.processInstanceKey}")
    }
}
