plugins {
    java
    id("org.springframework.boot") version "3.4.5"
    id("io.spring.dependency-management") version "1.1.7"
    id("maven-publish")
}

group = "Orochi"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

tasks.register<JavaExec>("runClass") {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set(project.findProperty("mainClass") as String?)
    jvmArgs = listOf("-Xms512m", "-Xmx1024m")
}

repositories {
    mavenCentral()
}


dependencies {
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")  // Add JPA support
    implementation("com.microsoft.sqlserver:mssql-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    testImplementation("org.springframework.security:spring-security-test")  // SQL Server driver
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("io.mailtrap:mailtrap-java:1.0.0")
    
    // Jackson modules for Java 8 date/time support
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.core:jackson-databind")

    // PDF Generation - updated libraries
    implementation("com.itextpdf:itextpdf:5.5.13.3")
    implementation("com.itextpdf:html2pdf:4.0.5")

    // Excel export dependencies
    implementation("org.apache.poi:poi:5.2.3")
    implementation("org.apache.poi:poi-ooxml:5.2.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Add this to your existing build.gradle.kts
tasks.register("syncPomFromGradle") {
    description = "Updates the pom.xml file from Gradle dependencies"
    group = "build"

    doLast {
        println("Syncing pom.xml with Gradle dependencies...")
        // This is a placeholder for actual implementation
        // In a real implementation, you'd need to parse the dependencies
        // and generate the pom.xml content
    }
}

// Optional - add a task to notify about Maven builds
tasks.register("mavenBuild") {
    description = "Information about Maven builds"
    group = "help"

    doLast {
        println("""
            Maven build is available via 'mvn' command.
            The pom.xml file must be kept in sync with build.gradle.kts.
            Run './gradlew syncPomFromGradle' after changing dependencies.
        """.trimIndent())
    }
}
