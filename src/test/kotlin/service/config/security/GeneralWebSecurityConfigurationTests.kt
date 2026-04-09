package service.config.security

import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestComponent
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.OK
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import service.config.security.Authorities.SCOPE_ACTUATOR
import service.config.security.Authorities.SCOPE_API
import service.config.security.GeneralWebSecurityConfigurationTests.SecurityTestController

@WebMvcTest(SecurityTestController::class)
@Import(SecurityTestController::class, WebSecurityConfiguration::class)
class GeneralWebSecurityConfigurationTests(
    @Autowired private val mockMvc: MockMvc
) {

    @TestComponent
    @RestController
    class SecurityTestController {

        @GetMapping("/api/foo", "/api/bar")
        fun get(@AuthenticationPrincipal user: Any) = user
    }

    @TestFactory
    @WithMockUser(authorities = [SCOPE_API])
    fun `users with just the API scope can access any api endpoints`() = allApiPathsReturnStatus(OK)

    @TestFactory
    @WithMockUser(authorities = [SCOPE_ACTUATOR])
    fun `users with just the ACTUATOR scope cannot access any api endpoints`() = allApiPathsReturnStatus(FORBIDDEN)

    @TestFactory
    @WithMockUser(authorities = [SCOPE_API, SCOPE_ACTUATOR])
    fun `users with the API and ACTUATOR scopes cam access any api endpoints`() = allApiPathsReturnStatus(OK)

    private fun allApiPathsReturnStatus(status: HttpStatus): List<DynamicTest> =
        listOf("/api/foo", "/api/bar")
            .map { path -> dynamicTest(path) { mockMvc.get(path).andExpect { status { isEqualTo(status.value()) } } } }
}
