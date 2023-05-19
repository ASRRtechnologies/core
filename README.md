
how to deploy

1. increase version in gradle.properties
2. run `./gradlew clean build`
3. run `./gradlew publishToMavenLocal`
4. run `./gradlew publishAllPublicationsToNexusRepository`
5. go to https://oss.sonatype.org/#stagingRepositories and close and release the repository
6. 