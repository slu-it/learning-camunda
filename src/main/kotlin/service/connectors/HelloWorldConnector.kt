package service.connectors

import io.camunda.connector.api.annotation.OutboundConnector
import io.camunda.connector.api.error.ConnectorException
import io.camunda.connector.api.outbound.OutboundConnectorContext
import io.camunda.connector.api.outbound.OutboundConnectorFunction
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
@OutboundConnector(
    name = "Hello World",
    type = "learning::hello-world-connector",
    inputVariables = ["message"]
)
class HelloWorldConnector(
    @Value($$"${testing.errors.enabled:true}") private val errorsEnabled: Boolean,
) : OutboundConnectorFunction {

    private val log = getLogger(javaClass)

    override fun execute(context: OutboundConnectorContext): Any? {
        val input = context.bindVariables(HelloWorldConnectorInput::class.java)
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
