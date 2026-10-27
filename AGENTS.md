# AGENTS.md — nl.asrr:core

Kotlin/Spring Boot library of shared building blocks for ASRR services.
Consumed as a Maven Central artifact: `nl.asrr:core`.

## What's in the box

- **auth/** — JWT-based auth: controllers, DTOs (Login/Register), `BasicUser`
  model, `RefreshToken` + `GenericRefreshTokenService`, config, JWT utils.
  Drop-in authentication for Spring Boot services.
- **gaia/** — Node/cluster reporting: communicator + service for reporting
  node info (including CPU) to a Gaia backend. DTOs are synced with backend.
  Includes a Spring Boot auto-configured `GaiaScheduler`: set `gaia.enabled=true`
  and `gaia.name=<app-identifier>` and a bean is registered that pings Gaia on
  a fixed interval (`gaia.interval-ms`, default 10s). No boilerplate scheduler
  needed in the consumer.
- **generics/** — Generic CRUD stack (controller + service + repository + DTO
  + model) to scaffold REST resources with minimal boilerplate.
- **exceptions/** — `GlobalExceptionHandler` (Spring `ProblemDetail`-based),
  plus `NotFoundException`, `DuplicateException`, `InvalidOperationException`,
  `ValidationException`.
- **id/** — `IdGenerator` (distributed ID generation) + `MachineInfo`.
- **validation/** — `EmailValidator` and related helpers.
- **models/entity/** — Shared base entity types.

Lessons from Alpha, generalised (each class documents the incident behind it):

- **generics/search/** — `DynamicSearch` (filters `field:value`, `field:in:A,B`, `field:null`,
  `field:gte:1`; `search=field:words`; `sort=a:desc;b`; `$`-keys rejected), `CsvExport` (formula
  injection safe). `ITenantCrudService.dynamicSearch()` / `dynamicSearchCsv()` add the tenant;
  `ISearchableTenantCrudController` exposes `/dynamic-search` and `/csv-export` (opt-in base class).
- **exceptions/** — `GlobalExceptionHandler` keeps 500 for real faults: refusals, stale writes and
  bad input are 4xx, a real fault logs once with a `requestId` that is returned, a client that went
  away gets nothing. Plus `OperationFailedException` (409) and `TypedDuplicateException`.
- **logging/** — `LogThrottle`, `maskEmail`, `isClientDisconnect`.
- **ops/** — `MigrationLog` (the `migration=… status=noop|applied…` line AURA reads from Loki) and
  `@RunsOnEveryInstance` (the opt-out from a ShedLock lock).
- **monitoring/** — Sentry without the starter: `SentryBootstrap` (DSN only from the environment),
  `RequestContextEventProcessor` (tenant tags, credentials stripped from URLs),
  `SentryTracingConfiguration` (`@Import` it), `CronMonitor` (cron check-ins for jobs whose silence
  matters). Sentry is `compileOnly`: add `io.sentry:sentry` (and `sentry-spring-7`) yourself.
- **crypto/** — `EncryptionService` (AES-256-GCM, `v1:` prefix) and `SecretFieldEncryptionListener`
  (encrypts listed Mongo fields at rest, lazily migrates plaintext).
- **security/**, **text/**, **captcha/**, **web/ratelimit/** — `NetworkSecurityUtils.isPublicAddress`
  (SSRF), `escapeHtml`, `slugify`, `truncateOnGraphemeBoundary` (never a lone surrogate in Mongo),
  `TurnstileVerifier`, `PublicRateLimitFilter` (off / shadow / enforce).
- **audit/** — `AuditLogEntry`, `AuditChanges` (BSON diff with secrets masked), `AuditLogWriter`
  (batched, off the request thread, TTL retention).
- **auth/revocation/**, **auth/apikey/** — `LiveSocketRegistry` + `SessionRevocationBus` (close a
  revoked session's WebSockets on every pod via Redis), `ApiKeyCodec` (prefix + CRC32 format, SHA-256).

These classes are plain classes, not beans: core is not component-scanned by consuming apps, so
declare the ones you use as `@Bean`s.

### Architecture contracts (test fixtures)

`testImplementation(testFixtures("nl.asrr:core:<version>"))`, then one subclass per contract in the
app's test tree, e.g. `class TenantIsolationTest : TenantIsolationContract() { override val basePackage = "nl.asrr.myapp" }`:

- `TenantIsolationContract` — every `@Document` has a `tenantId`, repository queries filter on it,
  no tenant-blind repository call (`findById`, …) from main code. Ratchet baselines in
  `src/test/resources/architecture/tenant-isolation/` may only shrink.
- `EndpointAuthorizationContract` — endpoints without `@PreAuthorize` or with only
  `isAuthenticated()` must be reviewed in `architecture/endpoints-open-to-any-user.txt`.
- `ScheduledJobLockContract` — every `@Scheduled` has a `@SchedulerLock` (unique name, explicit
  durations) or `@RunsOnEveryInstance("why")`.
- `BeanNameUniquenessContract`, `MigrationLogContract`, `BooleanJsonPropertyContract` (`isX`
  booleans need `@field:JsonProperty("isX")`).

## When to use it

Use this lib in any ASRR Spring Boot service that needs auth, standardized
error responses, CRUD scaffolding, ID generation, or Gaia reporting — instead
of reimplementing them.

## Stack

- Spring Boot 4, Kotlin 2.2, Gradle 9, JDK 21.
- Published via semantic-release on push to `dev` (conventional commits).

## Pointers for agents consuming the lib

- Public entry points live under `nl.asrr.core.{auth,gaia,generics,exceptions,id,validation,logging,ops,monitoring,crypto,security,text,captcha,web,audit}`.
- Prefer extending `generics/` base classes over rolling new CRUD.
- Auth expects a `BasicUser`-derived user model and JWT config wired via Spring.
- Throw the exceptions in `exceptions/` — `GlobalExceptionHandler` converts
  them to RFC 7807 `ProblemDetail` responses.
