package service.connectors

import io.camunda.connector.api.annotation.OutboundConnector
import io.camunda.connector.api.outbound.OutboundConnectorContext
import io.camunda.connector.api.outbound.OutboundConnectorFunction
import org.slf4j.LoggerFactory.getLogger
import org.springframework.stereotype.Component

@Component
@OutboundConnector(
    name = "Hello World",
    type = "learning::hello-world-connector",
)
class HelloWorldConnector : OutboundConnectorFunction {

    private val log = getLogger(javaClass)

    override fun execute(context: OutboundConnectorContext): Any? {
        val input = context.bindVariables(HelloWorldConnectorInput::class.java)
        log.info("Hello World Connector: ${input.inputMessage}")
        return null
    }
}
