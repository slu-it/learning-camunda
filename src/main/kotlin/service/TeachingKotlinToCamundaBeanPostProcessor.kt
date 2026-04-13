package service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.stereotype.Component

/**
 * At the moment (2026-04-14), Camunda Connectors are still using the Jackson 2 [ObjectMapper] without Kotlin support.
 * With this [BeanPostProcessor] we are registering a default [KotlinModule] for those.
 *
 * This is somewhat brute force, in a real project you might want to be more specific about which beans to modify
 * (e.g. by name), or replace the default Camunda beans by defining you own instances before auto-configuration is
 * executed. But for this example, the brute-force approach is enough.
 */
@Component
class TeachingKotlinToCamundaBeanPostProcessor : BeanPostProcessor {
    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        if (bean is ObjectMapper && beanName == "outboundConnectorObjectMapper") {
            return bean.registerModule(KotlinModule.Builder().build())
        }
        return bean
    }
}
