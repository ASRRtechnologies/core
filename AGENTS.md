# AGENTS.md — nl.asrr:core

Kotlin/Spring Boot library of shared building blocks for ASRR services.
Consumed as a Maven Central artifact: `nl.asrr:core`.

## What's in the box

- **auth/** — JWT-based auth: controllers, DTOs (Login/Register), `BasicUser`
  model, `RefreshToken` + `GenericRefreshTokenService`, config, JWT utils.
  Drop-in authentication for Spring Boot services.
- **gaia/** — Node/cluster reporting: communicator + service for reporting
  node info (including CPU) to a Gaia backend. DTOs are synced with backend.
- **generics/** — Generic CRUD stack (controller + service + repository + DTO
  + model) to scaffold REST resources with minimal boilerplate.
- **exceptions/** — `GlobalExceptionHandler` (Spring `ProblemDetail`-based),
  plus `NotFoundException`, `DuplicateException`, `InvalidOperationException`,
  `ValidationException`.
- **id/** — `IdGenerator` (distributed ID generation) + `MachineInfo`.
- **validation/** — `EmailValidator` and related helpers.
- **models/entity/** — Shared base entity types.

## When to use it

Use this lib in any ASRR Spring Boot service that needs auth, standardized
error responses, CRUD scaffolding, ID generation, or Gaia reporting — instead
of reimplementing them.

## Stack

- Spring Boot 4, Kotlin 2.2, Gradle 9, JDK 21.
- Published via semantic-release on push to `dev` (conventional commits).

## Pointers for agents consuming the lib

- Public entry points live under `nl.asrr.core.{auth,gaia,generics,exceptions,id,validation}`.
- Prefer extending `generics/` base classes over rolling new CRUD.
- Auth expects a `BasicUser`-derived user model and JWT config wired via Spring.
- Throw the exceptions in `exceptions/` — `GlobalExceptionHandler` converts
  them to RFC 7807 `ProblemDetail` responses.
