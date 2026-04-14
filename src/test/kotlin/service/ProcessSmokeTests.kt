package service

import io.camunda.client.CamundaClient
import io.camunda.client.api.response.ProcessInstanceEvent
import io.camunda.client.api.search.response.UserTask
import io.camunda.process.test.api.CamundaAssert
import io.camunda.process.test.api.CamundaSpringProcessTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import service.ProcessSmokeTests.CamundaHelper

@ActiveProfiles("test")
@CamundaSpringProcessTest
@Import(CamundaHelper::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
class ProcessSmokeTests(
    @Autowired private val camunda: CamundaHelper
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

    @Test
    fun `hello world connector process can be completed`() {
        with(camunda) {
            addResources("models/hello-world-connector.bpmn")

            val instance = createInstance("process_hello-world-connector")

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

    class CamundaHelper(
        private val client: CamundaClient,
    ) {

        fun addResources(vararg resources: String) {
            if (resources.isEmpty()) return

            client.newDeployResourceCommand()
                .let { resources.map { resource -> it.addResourceFile("src/camunda/$resource") }.last() }
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
}
