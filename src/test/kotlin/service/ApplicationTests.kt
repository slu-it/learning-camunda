package service

import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalManagementPort
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.client.RestTestClient

@ActiveProfiles("test")
@AutoConfigureRestTestClient
@SpringBootTest(webEnvironment = RANDOM_PORT)
class ApplicationTests(
    @Autowired val client: RestTestClient
) {

    @Nested
    inner class ActuatorSecurity(
        @LocalManagementPort private val port: Int
    ) {

        val publicEndpoints = setOf("/actuator/info", "/actuator/health")
        val privateEndpoints = setOf("/actuator/metrics")

        @TestFactory
        fun `without credentials only public endpoints are available`() =
            dynamicTests(publicEndpoints to OK, privateEndpoints to UNAUTHORIZED) { endpoint, status ->
                assertThatBasicAuthUserReturnsStatus(endpoint, status)
            }

        @TestFactory
        fun `with credentials of user with ACTUATOR scope all endpoints are available`() =
            dynamicTests(publicEndpoints to OK, privateEndpoints to OK) { endpoint, status ->
                assertThatBasicAuthUserReturnsStatus(endpoint, status, "actuator")
            }

        @TestFactory
        fun `with credentials of user without ACTUATOR scope only public endpoints are available`() =
            dynamicTests(publicEndpoints to OK, privateEndpoints to FORBIDDEN) { endpoint, status ->
                assertThatBasicAuthUserReturnsStatus(endpoint, status, "user")
            }

        private fun dynamicTests(
            vararg expectations: Pair<Set<String>, HttpStatus>,
            block: (String, HttpStatus) -> Unit
        ) = mapOf(*expectations)
            .flatMap { (endpoints, status) -> endpoints.map { it to status } }
            .map { (endpoint, status) -> dynamicTest("$endpoint >> $status") { block(endpoint, status) } }

        private fun assertThatBasicAuthUserReturnsStatus(path: String, status: HttpStatus, username: String? = null) =
            client.get()
                .uri { builder -> builder.port(port).path(path).build() }
                .headers { if (username != null) it.setBasicAuth(username, username.reversed()) }
                .exchange()
                .expectStatus().isEqualTo(status)
    }
}
