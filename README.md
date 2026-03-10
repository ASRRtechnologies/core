# ASRR Core Kotlin Library

A library for all common ASRR code.

[![Maven Central](https://img.shields.io/maven-central/v/nl.asrr/core)](https://central.sonatype.com/artifact/nl.asrr/core)

## Installation

### Maven

```xml
<dependency>
    <groupId>nl.asrr</groupId>
    <artifactId>core</artifactId>
    <version><!-- version --></version>
</dependency>
```

## Deployment

Releases are automated via [semantic-release](https://github.com/semantic-release/semantic-release) and GitHub Actions. On every push to `dev` with a [conventional commit](https://www.conventionalcommits.org/) message, a new version is automatically:

1. Tagged and released on GitHub
2. Published to [Maven Central](https://central.sonatype.com/artifact/nl.asrr/core)

### Commit message format

| Prefix | Release type |
|--------|-------------|
| `fix:` | Patch (0.0.x) |
| `feat:` | Minor (0.x.0) |
| `feat!:` or `BREAKING CHANGE:` | Major (x.0.0) |
