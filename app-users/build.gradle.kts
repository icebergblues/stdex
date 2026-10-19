plugins {
    java
    id("io.quarkus") version "3.37.2"
    id("io.freefair.lombok") version "9.2.0"
}

repositories {
    mavenCentral()
    mavenLocal()
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-hibernate-orm")
    implementation("io.quarkus:quarkus-rest-client-jsonb")
    implementation("io.quarkus:quarkus-flyway")
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
    implementation("io.quarkus:quarkus-rest-jsonb")
    implementation("io.quarkus:quarkus-rest-client")
    implementation("io.quarkus:quarkus-smallrye-fault-tolerance")
    implementation("io.quarkus:quarkus-hibernate-orm-panache")
    implementation("io.quarkus:quarkus-jackson")
    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("io.quarkus:quarkus-smallrye-stork")
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-rest-client-jackson")

	implementation("io.quarkus:quarkus-jdbc-postgresql")
    
// Source: https://mvnrepository.com/artifact/org.flywaydb/flyway-database-postgresql
    runtimeOnly("org.flywaydb:flyway-database-postgresql:12.5.0")
    // Source: https://mvnrepository.com/artifact/io.smallrye.reactive/smallrye-mutiny-vertx-consul-client
    implementation("io.smallrye.reactive:smallrye-mutiny-vertx-consul-client")
    // Source: https://mvnrepository.com/artifact/io.smallrye.stork/stork-service-discovery-consul
    implementation("io.smallrye.stork:stork-service-discovery-consul")
// Source: https://mvnrepository.com/artifact/io.smallrye.stork/stork-service-discovery-static-list
    implementation("io.smallrye.stork:stork-service-discovery-static-list:3.0.3")


    testImplementation("io.quarkus:quarkus-junit")
    testImplementation("io.rest-assured:rest-assured")
}

group = "com.uce.programacions"
version = "1.0.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}
