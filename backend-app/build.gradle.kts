import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "2.5.5"
	id("io.spring.dependency-management") version "1.0.11.RELEASE"
	id("org.jetbrains.kotlin.kapt") version "1.5.31"

	kotlin("jvm") version "1.5.31"
	kotlin("plugin.spring") version "1.5.31"
	kotlin("plugin.serialization") version "1.5.31"
}

group = "pt.unl.fct.scc"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.security:spring-security-crypto")

	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
	implementation("io.projectreactor:reactor-core")
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")

	implementation("com.azure:azure-storage-blob:12.13.0")

	implementation("io.lettuce:lettuce-core:6.1.5.RELEASE")

	implementation("org.litote.kmongo:kmongo-serialization:4.3.0")
	implementation("org.litote.kmongo:kmongo-coroutine-serialization:4.3.0")
	implementation("org.litote.kmongo:kmongo-id-serialization:4.3.0")
	kapt("org.litote.kmongo:kmongo-annotation-processor:4.3.0")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf("-Xjsr305=strict", "-Xopt-in=io.lettuce.core.ExperimentalLettuceCoroutinesApi")
		jvmTarget = "11"
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
