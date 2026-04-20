package service.connectors

import io.camunda.connector.api.annotation.InboundConnector
import io.camunda.connector.api.inbound.CorrelationFailureHandlingStrategy.ForwardErrorToUpstream
import io.camunda.connector.api.inbound.CorrelationFailureHandlingStrategy.Ignore
import io.camunda.connector.api.inbound.CorrelationRequest
import io.camunda.connector.api.inbound.CorrelationResult.Failure
import io.camunda.connector.api.inbound.CorrelationResult.Success
import io.camunda.connector.api.inbound.InboundConnectorContext
import io.camunda.connector.api.inbound.InboundConnectorExecutable
import io.camunda.connector.generator.java.annotation.BpmnType.START_EVENT
import io.camunda.connector.generator.java.annotation.ElementTemplate
import io.camunda.connector.generator.java.annotation.ElementTemplate.ConnectorElementType
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@InboundConnector(
    name = "Hello World Inbound Connector",
    type = "learning::hello-world-inbound-connector",
)
@ElementTemplate(
    id = "learning.hello-world-inbound-connector",
    name = "Hello World Inbound Connector",
    description = "A simple inbound connector.",
    elementTypes = [
        ConnectorElementType(
            appliesTo = [START_EVENT],
            elementType = START_EVENT,
        ),
    ],
    defaultResultVariable = "event",
)
@Scope(SCOPE_PROTOTYPE)
class HelloWorldInboundConnector(
    private val eventBridge: HelloWorldInboundConnectorEventBridge,
) : InboundConnectorExecutable<InboundConnectorContext> {

    private val log = getLogger(javaClass)
    private var context: InboundConnectorContext? = null

    override fun activate(context: InboundConnectorContext) {
        this.context = context
        eventBridge.add(this)
        log.info("Hello World Inbound Connector activated")
    }

    override fun deactivate() {
        this.context = null
        eventBridge.remove(this)
        log.info("Hello World Inbound Connector deactivated")
    }

    fun handle(event: HelloWorldDto) {
        log.info("Received event with message: ${event.message}")
        val ctx = context ?: error("context is null")

        val request = CorrelationRequest.builder()
            .variables(mapOf("message" to event.message))
            .build()

        when (val result = ctx.correlate(request)) {
            is Success -> log.info("Correlation succeeded")
            is Failure -> when (result.handlingStrategy()) {
                is ForwardErrorToUpstream -> error("Correlation failed!")
                is Ignore -> log.info("Correlation failed but strategy is Ignore, skipping")
            }
        }
    }
}
