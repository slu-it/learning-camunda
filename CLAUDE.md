# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
./gradlew build          # Full build (includes tests, detekt, kover coverage report)
./gradlew test           # Run tests only
./gradlew detekt         # Run static analysis
./gradlew koverHtmlReport  # Generate coverage report

# Run a single test class
./gradlew test --tests "service.config.security.GeneralWebSecurityConfigurationTests"
```

## Architecture

This is a **Kotlin + Spring Boot 4 application** that integrates with **Camunda 8** (self-managed) using the Camunda Job Worker pattern.

### Key Stack
- **Kotlin 2.2 / JVM 21** with Spring Boot 4
- **Camunda 8.9** via `camunda-spring-boot-starter` — connects to Zeebe via gRPC (`26500`) and REST (`8080`)
- **Spring Security** with HTTP Basic auth (stateless, no CSRF/CORS)
- **Logbook** for HTTP request/response logging
- **Detekt** for static analysis (`configurations/detekt.yml`), zero-tolerance (`maxIssues: 0`)
- **Kover** for code coverage (HTML report generated as part of `build`)

### Package Structure
```
service/
├── Application.kt                     # Spring Boot entry point
├── ApplicationConfiguration.kt        # Global beans (Clock, IdGenerator, Logbook sink)
├── config/security/
│   ├── Authorities.kt                 # Authority constants: SCOPE_API, SCOPE_ACTUATOR
│   ├── WebSecurityConfiguration.kt    # Two filter chains: actuator (Order 1), API (Order 2)
│   └── MethodSecurityConfiguration.kt # @EnableMethodSecurity
└── workers/
    └── HelloWorldWorker.kt            # @JobWorker for job type "learning::hello-world"
```

### Security Model
- Two `SecurityFilterChain` beans ordered by priority
- `/actuator/**`: requires `SCOPE_ACTUATOR` (health/info are public)
- `/api/**`: requires `SCOPE_API`
- In-memory users: `user`/`resu` (API scope), `actuator`/`rotautca` (actuator scope)
- Management server runs on port **9091**, app on **8081**

### Camunda Integration
- Workers are annotated with `@JobWorker(type = "learning::hello-world")`
- BPMN process `process_learning_hello-world` is defined in `src/test/resources/models/hello-world.bpmn`
- Process tests use `camunda-process-test-spring` with a Docker-based Camunda 8.9.0 container
- Test config in `src/test/resources/application-test.yml` disables Camunda health checks and sets the Camunda Docker image version

### Profiles
- `json-logging` profile: switches Logbook to JSON format with logback-json config
- `cloud` profile: enables `LogstashLogbackSink` for structured HTTP logging
