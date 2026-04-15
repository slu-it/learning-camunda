package service.api

import org.springframework.http.HttpStatus.NO_CONTENT
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import service.connectors.HelloWorldDto
import service.connectors.HelloWorldPublisher

@RestController
@RequestMapping("/api/hello-world")
class HelloWorldController(
    private val publisher: HelloWorldPublisher
) {

    @PostMapping
    @ResponseStatus(NO_CONTENT)
    fun receiveWebhook(@RequestBody payload: HelloWorldDto) {
        publisher.publish(payload)
    }
}
