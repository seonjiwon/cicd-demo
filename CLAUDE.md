# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 3.5 web application (Java 17) used as a CI/CD demonstration project. Uses Gradle 8.14 with the Gradle wrapper.

## Build & Dev Commands

```bash
./gradlew build          # Build + run all tests
./gradlew bootRun        # Run the application (default port 8080)
./gradlew test           # Run all tests (JUnit 5)
./gradlew test --tests "dev.fisa.SomeTestClass.someMethod"  # Run a single test
./gradlew clean build    # Clean and rebuild
```

## Architecture

- **Base package:** `dev.fisa` — Spring Boot component scan root
- **Build:** Gradle with Spring Boot plugin and Spring dependency management
- **Dependencies:** Spring Web, Lombok (compile-only with annotation processing), JUnit 5 for tests
- **Config:** `src/main/resources/application.yaml`