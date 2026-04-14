package service.workers

import io.camunda.client.annotation.JobWorker
import io.camunda.client.annotation.Variable
import io.camunda.client.api.response.ActivatedJob
import org.slf4j.LoggerFactory.getLogger
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.Currency

@Component
class ExpensesWorker {

    private val log = getLogger(javaClass)

    @JobWorker(type = "expenses::process")
    fun process(
        job: ActivatedJob,
        @Variable("employeeId") employeeId: String,
        @Variable("amount") amount: BigDecimal,
        @Variable("currency") currency: Currency,
    ) {
        log.info("Processed $amount $currency expenses for $employeeId -- ${job.processInstanceKey}")
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
