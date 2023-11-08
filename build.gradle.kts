<<<<<<< HEAD
plugins {
	java
	id("org.springframework.boot") version "3.1.3"
	id("io.spring.dependency-management") version "1.1.3"
}

group = "com.metaverse"
version = "0.0.1-SNAPSHOT"

java {
	sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.projectlombok:lombok:1.18.26")
	implementation("org.apache.logging.log4j:log4j-core:2.20.0")
	implementation("com.googlecode.json-simple:json-simple:1.1.1")
	annotationProcessor("org.projectlombok:lombok:1.18.22")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	
}

tasks.withType<Test> {
	useJUnitPlatform()
}
=======
plugins {
	java
	id("org.springframework.boot") version "3.1.3"
	id("io.spring.dependency-management") version "1.1.3"
}

group = "com.metaverse"
version = "0.0.1-SNAPSHOT"

java {
	sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.projectlombok:lombok:1.18.26")
	implementation("org.apache.logging.log4j:log4j-core:2.20.0")
	implementation("com.googlecode.json-simple:json-simple:1.1.1")
	annotationProcessor("org.projectlombok:lombok:1.18.22")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	
}

tasks.withType<Test> {
	useJUnitPlatform()
}
>>>>>>> 76a60616b63b422ee72c9381c75b65ab8609782e
