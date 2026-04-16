package service.connectors

import io.camunda.connector.api.annotation.OutboundConnector
import io.camunda.connector.api.error.ConnectorException
import io.camunda.connector.api.outbound.OutboundConnectorContext
import io.camunda.connector.api.outbound.OutboundConnectorFunction
import io.camunda.connector.generator.java.annotation.ElementTemplate
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
@OutboundConnector(
    name = "Hello World Outbound Connector",
    type = "learning::hello-world-outbound-connector",
    inputVariables = ["message"]
)
@ElementTemplate(
    id = "learning.hello-world-outbound-connector",
    name = "Hello World Outbound Connector",
    description = "A simple outbound connector.",
    inputDataClass = HelloWorldDto::class,
)
class HelloWorldOutboundConnector(
    @Value($$"${testing.errors.enabled:false}") private val errorsEnabled: Boolean,
) : OutboundConnectorFunction {

    private val log = getLogger(javaClass)

    override fun execute(context: OutboundConnectorContext): Any? {
        val input = context.bindVariables(HelloWorldDto::class.java)
        maybeThrowAnError()
        log.info("Hello World Connector: ${input.message}")
        return null
    }

    @Suppress("MagicNumber")
    private fun maybeThrowAnError() {
        if (errorsEnabled) {
            val chance = Random.nextInt(100)
            if (chance <= 10) {
                log.error("exception happened")
                error("oops")
            }
            if (chance <= 25) {
                log.error("BPMN error happened")
                throw ConnectorException("WORLD_ERROR", "World error occurred.")
            }
        }
    }
}
