package service

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.util.IdGenerator
import org.springframework.util.JdkIdGenerator
import org.zalando.logbook.HttpLogFormatter
import org.zalando.logbook.logstash.LogstashLogbackSink
import java.time.Clock

@Configuration
class ApplicationConfiguration {

    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun idGenerator(): IdGenerator = JdkIdGenerator()

    @Bean
    @Profile("cloud")
    fun httpMessageSink(httpLogFormatter: HttpLogFormatter) = LogstashLogbackSink(httpLogFormatter)
}
