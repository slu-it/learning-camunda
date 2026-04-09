package service.workers

import io.camunda.client.annotation.JobWorker
import io.camunda.client.api.response.ActivatedJob
import org.slf4j.LoggerFactory.getLogger
import org.springframework.stereotype.Component

@Component
class HelloWorldWorker {

    private val log = getLogger(javaClass)

    @JobWorker(type = "learning::hello-world")
    fun handleHelloWorld(job: ActivatedJob) {
        log.info("Hello ${job.getVariable("inputName") ?: "World"}!")
        return
    }
}
