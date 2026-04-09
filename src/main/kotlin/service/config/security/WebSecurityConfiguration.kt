package service.config.security

import org.springframework.boot.actuate.info.InfoEndpoint
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.HttpSecurityDsl
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import service.config.security.Authorities.SCOPE_ACTUATOR
import service.config.security.Authorities.SCOPE_API

@Configuration
@EnableWebSecurity
class WebSecurityConfiguration {

    private val passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

    @Bean
    @Order(1)
    fun actuatorSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            securityMatcher(EndpointRequest.toAnyEndpoint())
            applyDefaults()

            httpBasic {}
            authorizeHttpRequests {
                authorize(EndpointRequest.to(InfoEndpoint::class.java, HealthEndpoint::class.java), permitAll)
                authorize(EndpointRequest.toAnyEndpoint(), hasAuthority(SCOPE_ACTUATOR))
                authorize(anyRequest, denyAll)
            }
        }
        return http.build()
    }

    @Bean
    @Order(2)
    fun generalSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            securityMatcher("/**")
            applyDefaults()

            httpBasic {}
            authorizeHttpRequests {
                authorize("/api/**", hasAuthority(SCOPE_API))
                authorize("/error", permitAll)
                authorize(anyRequest, denyAll)
            }
        }
        return http.build()
    }

    private fun HttpSecurityDsl.applyDefaults() {
        cors { disable() }
        csrf { disable() }
        sessionManagement {
            sessionCreationPolicy = SessionCreationPolicy.STATELESS
        }
    }

    @Bean
    fun userDetailService(): UserDetailsService =
        InMemoryUserDetailsManager(
            dummyUser("user", SCOPE_API),
            dummyUser("actuator", SCOPE_ACTUATOR)
        )

    private fun dummyUser(username: String, vararg authorities: String) =
        User.withUsername(username)
            .password(passwordEncoder.encode(username.reversed()))
            .authorities(*authorities)
            .build()
}
