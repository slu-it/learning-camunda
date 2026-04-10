package service

import io.camunda.client.CamundaClient
import io.camunda.client.api.response.ProcessInstanceEvent
import io.camunda.client.api.search.response.UserTask
import io.camunda.process.test.api.CamundaAssert
import io.camunda.process.test.api.CamundaSpringProcessTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalManagementPort
import org.springframework.context.annotation.Import
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.OK
import org.springframework.http.HttpStatus.UNAUTHORIZED
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.client.RestTestClient

@ActiveProfiles("test")
@CamundaSpringProcessTest
@AutoConfigureRestTestClient
@Import(CamundaHelper::class)
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

    @Nested
    inner class WorkerSmokeTests(
        @Autowired private val camunda: CamundaHelper,
    ) {

        @Test
        fun `hello world example process can be completed`() {
            with(camunda) {
                addResources("models/hello-world.bpmn", "models/hello-world.form")

                val instance = createInstance("process_learning_hello-world")

                waitForUserTaskAndSubmit(instance, "activity_form-input", mapOf("name" to "Tester"))
                waitForCompletion(instance)
            }
        }

        @Nested
        inner class ExpensesProcess {

            @BeforeEach
            fun setupProcess() {
                with(camunda) {
                    addResources(
                        "models/expenses.bpmn",
                        "models/expenses-approval-needed.dmn",
                        "models/expenses-input.form",
                        "models/expenses-approval.form",
                    )
                }
            }

            @Test
            fun `expenses process completes without approval when amount is less than 25 EUR`() {
                with(camunda) {
                    val instance = createInstance("process_expenses")

                    waitForUserTaskAndSubmit(
                        instance = instance,
                        elementId = "userTask_submitForm",
                        variables = mapOf(
                            "inputEmployeeId" to "EID-4711",
                            "inputDate" to "2026-04-10",
                            "inputTitle" to "Coffee",
                            "inputAmount" to 15,
                            "inputCurrency" to "EUR",
                            "inputVat" to "DEFAULT_19",
                        )
                    )

                    waitForCompletion(instance)
                    assertHasNonActivatedElements(instance, "userTask_approval")
                    assertHasCompletedElements(instance, "endEvent_processed")
                }
            }

            @Test
            fun `expenses process routes to approval task when amount is more than 25 EUR`() {
                with(camunda) {
                    val instance = createInstance("process_expenses")

                    waitForUserTaskAndSubmit(
                        instance = instance,
                        elementId = "userTask_submitForm",
                        variables = mapOf(
                            "inputEmployeeId" to "EID-4711",
                            "inputDate" to "2026-04-10",
                            "inputTitle" to "Team Dinner",
                            "inputAmount" to 100,
                            "inputCurrency" to "EUR",
                            "inputVat" to "DEFAULT_19",
                        )
                    )

                    waitForUserTaskAndSubmit(
                        instance = instance,
                        elementId = "userTask_approval",
                        variables = mapOf("approvalDecision" to "true"),
                    )

                    waitForCompletion(instance)
                    assertHasCompletedElements(instance, "endEvent_processed")
                }
            }
        }
    }
}

class CamundaHelper(
    private val client: CamundaClient,
) {

    fun addResources(vararg resources: String) {
        if (resources.isEmpty()) return

        client.newDeployResourceCommand()
            .let { resources.map { resource -> it.addResourceFromClasspath(resource) }.last() }
            .send()
            .join()
    }

    fun createInstance(processId: String) =
        client.newCreateInstanceCommand()
            .bpmnProcessId(processId)
            .latestVersion()
            .send()
            .join()

    fun waitForUserTaskAndSubmit(
        instance: ProcessInstanceEvent,
        elementId: String,
        variables: Map<String, Any> = emptyMap()
    ) {
        waitForElementToBecomeActive(instance, elementId)
        val task = getUserTask(instance, elementId)
        complete(task, variables)
    }

    fun getUserTask(instance: ProcessInstanceEvent, activityId: String) =
        client.newUserTaskSearchRequest()
            .filter { it.processInstanceKey(instance.processInstanceKey).elementId(activityId) }
            .send()
            .join()
            .items()
            .single()

    fun complete(userTask: UserTask, variables: Map<String, Any> = emptyMap()) =
        client.newCompleteUserTaskCommand(userTask.userTaskKey)
            .variables(variables)
            .send()
            .join()

    fun waitForElementToBecomeActive(instance: ProcessInstanceEvent, elementId: String) {
        CamundaAssert.assertThat(instance).hasActiveElements(elementId)
    }

    fun waitForCompletion(instance: ProcessInstanceEvent) {
        CamundaAssert.assertThat(instance).isCompleted()
    }

    fun assertHasCompletedElements(instance: ProcessInstanceEvent, vararg elementIds: String) {
        CamundaAssert.assertThat(instance).hasCompletedElements(*elementIds)
    }

    fun assertHasNonActivatedElements(instance: ProcessInstanceEvent, vararg elementIds: String) {
        CamundaAssert.assertThat(instance).hasNotActivatedElements(*elementIds)
    }
}
