package service.experiments.api

import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import service.experiments.connectors.HelloWorldDto
import service.experiments.connectors.HelloWorldInboundConnectorEventBridge

@RestController
@RequestMapping("/api/hello-world")
class HelloWorldController(
    private val bridge: HelloWorldInboundConnectorEventBridge
) {

    @PostMapping
    @ResponseStatus(NO_CONTENT)
    fun receiveWebhook(@RequestBody payload: HelloWorldDto) {
        bridge.handle(payload)
    }
}
