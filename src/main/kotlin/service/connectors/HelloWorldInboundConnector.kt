package service.connectors

import io.camunda.connector.api.annotation.InboundConnector
import io.camunda.connector.api.inbound.CorrelationFailureHandlingStrategy.ForwardErrorToUpstream
import io.camunda.connector.api.inbound.CorrelationFailureHandlingStrategy.Ignore
import io.camunda.connector.api.inbound.CorrelationRequest
import io.camunda.connector.api.inbound.CorrelationResult.Failure
import io.camunda.connector.api.inbound.CorrelationResult.Success
import io.camunda.connector.api.inbound.InboundConnectorContext
import io.camunda.connector.api.inbound.InboundConnectorExecutable
import org.slf4j.LoggerFactory.getLogger
import org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component

@Component
@InboundConnector(
    name = "Hello World Inbound Connector",
    type = "learning::hello-world-inbound-connector",
)
@Scope(SCOPE_PROTOTYPE)
class HelloWorldInboundConnector(
    private val publisher: HelloWorldPublisher,
) : InboundConnectorExecutable<InboundConnectorContext>, HelloWorldWebhookSubscriber {

    private val log = getLogger(javaClass)
    private lateinit var context: InboundConnectorContext

    override fun activate(context: InboundConnectorContext) {
        this.context = context
        publisher.subscribe(this)
        log.info("Hello World Inbound Connector activated")
    }

    override fun deactivate() {
        publisher.unsubscribe(this)
        log.info("Hello World Inbound Connector deactivated")
    }

    override fun handleWebhook(payload: HelloWorldDto) {
        log.info("Received webhook message: ${payload.message}")

        val request = CorrelationRequest.builder()
            .variables(mapOf("message" to payload.message))
            .build()

        when (val result = context.correlate(request)) {
            is Success -> log.info("Correlation succeeded")
            is Failure -> when (result.handlingStrategy()) {
                is ForwardErrorToUpstream -> error("Correlation failed!")
                is Ignore -> log.info("Correlation failed but strategy is Ignore, skipping")
            }
        }
    }
}

interface HelloWorldWebhookSubscriber {
    fun handleWebhook(payload: HelloWorldDto)
}

@Component
class HelloWorldPublisher {

    private val subscribers = mutableListOf<HelloWorldWebhookSubscriber>()

    fun publish(event: HelloWorldDto) {
        subscribers.forEach { subscriber -> subscriber.handleWebhook(event) }
    }

    fun subscribe(subscriber: HelloWorldWebhookSubscriber) {
        subscribers.add(subscriber)
    }

    fun unsubscribe(subscriber: HelloWorldWebhookSubscriber) {
        subscribers.remove(subscriber)
    }
}
