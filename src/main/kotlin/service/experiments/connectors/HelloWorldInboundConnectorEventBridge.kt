package service.experiments.connectors

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class HelloWorldInboundConnectorEventBridge {

    private val connectors: MutableSet<HelloWorldInboundConnector> = ConcurrentHashMap.newKeySet()

    fun handle(event: HelloWorldDto) {
        connectors.forEach { connector -> connector.handle(event) }
    }

    fun add(connector: HelloWorldInboundConnector) {
        connectors.add(connector)
    }

    fun remove(connector: HelloWorldInboundConnector) {
        connectors.remove(connector)
    }
}
