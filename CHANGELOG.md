# ASRR Core Kotlin Package Changelog

## [1.3.0](https://github.com/ASRRtechnologies/core/compare/v1.2.2...v1.3.0) (2026-07-08)

## [1.2.2](https://github.com/ASRRtechnologies/core/compare/v1.2.1...v1.2.2) (2026-07-07)

## [1.2.1](https://github.com/ASRRtechnologies/core/compare/v1.2.1%0Dv1.2.0#diff) (2026-04-15)

### Bug Fixes

* **gaia:** catch Throwable so OSHI/JNA NoClassDefFoundError doesn't kill the scheduler ([42d5a0d](https://github.com/ASRRtechnologies/core/commits/42d5a0d9c505924514a209a2629b4879fb059dc8))

## [1.2.0](https://github.com/ASRRtechnologies/core/compare/v1.2.0%0Dv1.1.0#diff) (2026-04-14)

### Features

* **gaia:** auto-configured scheduler and optional version reporting ([6510b2f](https://github.com/ASRRtechnologies/core/commits/6510b2f8564db70733f8ec4e93cbc2036b69ec2f))

## [1.1.0](https://github.com/ASRRtechnologies/core/compare/v1.1.0%0Dv1.0.1#diff) (2026-04-14)

### Features

* **gaia:** sync dtos with backend and report cpu info ([7777633](https://github.com/ASRRtechnologies/core/commits/7777633f7af671769428d99a26757ffd25c622de))

### Bug Fixes

* **ci:** bump CI JDK to 21 to match project toolchain ([24d63bf](https://github.com/ASRRtechnologies/core/commits/24d63bf4052520c2b75522a0be1bb335f97e9f10))

## [1.0.1](https://github.com/ASRRtechnologies/core/compare/v1.0.1%0Dv1.0.0#diff) (2026-04-14)

### Bug Fixes

* **build:** add versionMapping so resolved dep versions land in published pom ([8bdef91](https://github.com/ASRRtechnologies/core/commits/8bdef919f228d94bbda9fa39b76ace75a6a22c8a))

## [1.0.0](https://github.com/ASRRtechnologies/core/compare/v1.0.0%0Dv0.2.6#diff) (2026-04-14)

### ⚠ BREAKING CHANGES

* Consumers must be on Spring Boot 3.4 / Kotlin 2.1 / JDK 17+.
The jjwt secret must now be at least 64 bytes for HS512. The kotlin-logging
import changed from mu.KotlinLogging to io.github.oshai.kotlinlogging.KotlinLogging.

### Features

* upgrade core stack and add ProblemDetail handler + PageResponse ([1e51a5a](https://github.com/ASRRtechnologies/core/commits/1e51a5a902b1f76485a44130e753d0fede350d2e))

## [0.2.6](https://github.com/ASRRtechnologies/core/compare/v0.2.6%0Dv0.2.5#diff) (2026-03-09)

### Bug Fixes

* actually use semantic versioning on publish (please let this be the final commit) ([86f32c5](https://github.com/ASRRtechnologies/core/commits/86f32c5eea44b10f9aa5ace297b7e67f76a0f981))

## [0.2.5](https://github.com/ASRRtechnologies/core/compare/v0.2.5%0Dv0.2.4#diff) (2026-03-09)

### Bug Fixes

* upgrade gradle to 8.5 and update creds ([a8e5ce8](https://github.com/ASRRtechnologies/core/commits/a8e5ce8e7119cffb1657820dc313dcd2874f5787))

# Core Changelog

## [0.2.4](https://github.com/ASRRtechnologies/core/compare/v0.2.4%0Dv0.2.3#diff) (2026-03-09)

### Bug Fixes

* migrate to proper maven central publishing setup ([c44a671](https://github.com/ASRRtechnologies/core/commits/c44a6719bbb73fc3910c47d057655334231d962f))

## [0.2.3](https://github.com/ASRRtechnologies/core/compare/v0.2.3%0Dv0.2.2#diff) (2026-03-09)

### Bug Fixes

* use diff useInMemoryPgpKeys signature ([e016399](https://github.com/ASRRtechnologies/core/commits/e016399e96f48663fd351a958eee0e3c41fb75e7))

## [0.2.2](https://github.com/ASRRtechnologies/core/compare/v0.2.2%0Dv0.2.1#diff) (2026-03-09)

### Bug Fixes

* configure maven central publishing ([fda8592](https://github.com/ASRRtechnologies/core/commits/fda859220bdb82ac0213b6cbf7ff9150b7e73bc3))
* remove incorrect API for maven central publishing ([d469f39](https://github.com/ASRRtechnologies/core/commits/d469f39593608aaf1ccfd9833adec090b259071b))

## [0.2.1](https://github.com/ASRRtechnologies/core/compare/v0.2.1%0Dv0.2.0#diff) (2026-03-09)

### Bug Fixes

* revert back to using GITHUB_TOKEN ([b74a11a](https://github.com/ASRRtechnologies/core/commits/b74a11a2cbcba1e26fd46476e365173939ee830c))

## [0.2.0](https://github.com/ASRRtechnologies/core/compare/v0.2.0%0Dv0.1.3#diff) (2026-03-09)

### Features

* **generics:** add generic search + remove cosmos auth specific annotations from tenant crud ([e357d74](https://github.com/ASRRtechnologies/core/commits/e357d74efaaf25d250c6e7f972ffda0b249d63ca))
* **generics:** add produces tag to improve swagger output ([0b9a12f](https://github.com/ASRRtechnologies/core/commits/0b9a12ffece6f27a0fabf189feb6c74d49a9d4ed))
* **generics:** add save ([266df30](https://github.com/ASRRtechnologies/core/commits/266df30fe7737e3e4be8f097a6c9f2bddacc892f))
* **generics:** dedupe controller function names ([5aecd25](https://github.com/ASRRtechnologies/core/commits/5aecd2502f391ac8a1a87603a0e0e7ed6f8d50fb))
* **generics:** make save function open ([fcd4c27](https://github.com/ASRRtechnologies/core/commits/fcd4c273f0b7e177863719cd78bd3cc3719ca559))
* **repository:** update ITenantCrudRepository ([c78c084](https://github.com/ASRRtechnologies/core/commits/c78c084b4e0f4a0fef459bd33f1ffa1eb9815401))
* **semantic:** add nexus deploy step ([5f13fd8](https://github.com/ASRRtechnologies/core/commits/5f13fd80b718f2d15cd342b71d407d63d3cd163e))
* **semantic:** add semantic release to automatically increase gradle.properties version ([68c10be](https://github.com/ASRRtechnologies/core/commits/68c10beff8a2a59ffa3418c20807e7d6f8d7a4c3))

### Bug Fixes

* **generics:** change tenant crud find page, rename duplicate error ([0399bc6](https://github.com/ASRRtechnologies/core/commits/0399bc6e3aa0b0958d71a77bd3b2ccb8571c3572))
* **generics:** fix bug in tenantCrudcontroller ([826a9dc](https://github.com/ASRRtechnologies/core/commits/826a9dc0e83a4c3f9a4ec239f3394e3f8d0fa98a))
* **generics:** improve exists function and fix request params for findlist ([2bae7fc](https://github.com/ASRRtechnologies/core/commits/2bae7fc05953a230a0677e0e460f7cf1e3456115))
* **generics:** remove duplicate by in tenantcrudrepository ([3c0b399](https://github.com/ASRRtechnologies/core/commits/3c0b3996dbe69e9604f310e1ebadc8952c60ebee))
* **generics:** remove wrong 'and' in tenantcrudrepository ([b567746](https://github.com/ASRRtechnologies/core/commits/b567746a6306a97e437b5220045f14458c1de911))
* **generics:** update tenant controller and functions ([bd8abe2](https://github.com/ASRRtechnologies/core/commits/bd8abe2b0e5e253a0a2062896d6690431b5d8dc1))
* **publish:** add pgp key ([7dfa777](https://github.com/ASRRtechnologies/core/commits/7dfa7774eb9e4ce3c40ade25873eab6d1ddf7534))
* **publish:** add pgp key ([2c815b5](https://github.com/ASRRtechnologies/core/commits/2c815b59b5cfd46c4c34715cb6208e302e3876e7))
* **publish:** change pw retrieval method ([dd9e540](https://github.com/ASRRtechnologies/core/commits/dd9e540e15f6e09731b16bdde27525a70456ac0d))
* **publish:** remove wrong working directory ([e607b39](https://github.com/ASRRtechnologies/core/commits/e607b3944447f82e95246d0c5a15c0b78a026ef0))
* **search:** release generic search ([9eba871](https://github.com/ASRRtechnologies/core/commits/9eba871910ff0f0babb9bb278fa9508033a4600d))
* **search:** update search ([0d6525e](https://github.com/ASRRtechnologies/core/commits/0d6525e22919465ba3e605a70de05656b83e1866))
* **security:** add securityservice as dependency to ICrudService to auto update updatedBy on save ([2c41be0](https://github.com/ASRRtechnologies/core/commits/2c41be0679c77e41d0a45e0f6f9f0fd30a202198))
* **tenant-generics:** fix issue with find list ([b9145cc](https://github.com/ASRRtechnologies/core/commits/b9145cc59dfa54b46a5a45d97bdcec6b0b6d20c9))
* **tenant:** Inject tenant ID in overridden crud controller endpoints ([dc99d17](https://github.com/ASRRtechnologies/core/commits/dc99d17a49482f1b1a450d2b33a826b6073f0f04))
* **tenant:** make securityservice open ([bc1f087](https://github.com/ASRRtechnologies/core/commits/bc1f087b4557b6f194ca9909fc6ed2de0c780ce7))
* **tenant:** remove old pathvar tenant crud controller endpoints ([2e8acba](https://github.com/ASRRtechnologies/core/commits/2e8acba4d96875c2447883de7fe814865ba56156))
* **tenant:** simplify service (no explicit tenantId params anymore) ([784335b](https://github.com/ASRRtechnologies/core/commits/784335b1a8241dc5896596e36cc6d9a7bb08c562))
