package service.experiments.connectors

import io.camunda.connector.generator.java.annotation.TemplateProperty
import io.camunda.connector.generator.java.annotation.TemplateProperty.PropertyConstraints
import io.camunda.connector.generator.java.annotation.TemplateProperty.PropertyType

data class HelloWorldDto(
    @TemplateProperty(
        label = "Message",
        description = "The message to log.",
        type = PropertyType.String,
        optional = false,
        constraints = PropertyConstraints(notEmpty = true),
    )
    val message: String,
)
