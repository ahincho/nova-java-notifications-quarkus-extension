plugins {
    id("java-library")
    id("maven-publish")
    jacoco
    checkstyle
}

import java.io.File as JFile
import java.io.FileOutputStream as JFOS
import java.io.InputStream as JInputStream
import java.io.OutputStream as JOS
import java.net.URLClassLoader

group = findProperty("group") as String
version = findProperty("version") as String

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenLocal()
    mavenCentral()
    // GitHub Packages of nova-java-notifications (the pure library this extension
    // adapts to Quarkus). Without this entry, ./gradlew publish fails at
    // compileJava because it cannot resolve pe.edu.nova.java.libs:nova-notifications
    // from Maven Central (which only mirrors Maven-published artifacts, not
    // GitHub Packages). NOVA_PACKAGES_READ_TOKEN is a PAT with packages:read
    // scope; falls back to GITHUB_TOKEN if not set (GITHUB_TOKEN can read
    // packages within the same repo but not across repos, so the cross-repo
    // dependency on nova-java-notifications would fail without the PAT).
    // The content {} filter scopes this repo to pe.edu.nova.java.* coordinates
    // only; otherwise every missing artifact on Maven Central would 401 on
    // GitHub Packages and break resolution (Gradle treats 401 as "fail", not
    // "try the next repo").
    maven {
        name = "GitHubPackages-NovaNotifications"
        url = uri("https://maven.pkg.github.com/ahincho/nova-java-notifications")
        content {
            includeGroupByRegex("pe\\.edu\\.nova\\.java.*")
        }
        val token = System.getenv("NOVA_PACKAGES_READ_TOKEN")
            ?: System.getenv("NOVA_RELEASE_PAT")
            ?: System.getenv("GITHUB_TOKEN")
        if (!token.isNullOrBlank()) {
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: "x-access-token"
                password = token
            }
        }
    }
}

val junitVersion = "6.0.0"
val assertjVersion = "3.26.3"

dependencies {
    // Quarkus BOM aligns all extension versions.
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))

    // Quarkus ARC (CDI) — needed for @ApplicationScoped, @Singleton, @Inject, @Produces.
    implementation("io.quarkus:quarkus-arc")
    // Quarkus SmallRye Config — needed for @ConfigMapping.
    implementation("io.quarkus:quarkus-config-yaml")

    // Jandex runtime: used by the build-time index generation task below to
    // ship META-INF/jandex.idx in this JAR. Without it, Quarkus apps cannot
    // discover @Singleton beans declared in this extension JAR (CDI build-time
    // scan only sees classes that appear in some jandex.idx on the classpath).
    // NB: org.jboss:jandex on Maven Central is a parent POM only (no .jar).
    // The actual jar with org.jboss.jandex.Indexer ships as io.smallrye:jandex.
    implementation("io.smallrye:jandex:3.2.7")

    // The pure library this extension adapts to Quarkus.
    // artifactId is `nova-notifications` (no `java-` segment) — Nova convention:
    // only the groupId includes `java`, the artifactId is `nova-<role>`.
    api("pe.edu.nova.java.libs:nova-notifications:1.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("org.junit.platform:junit-platform-launcher:$junitVersion")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }
    }
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml"))
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)
    // 0.40 threshold: realistic for a Quarkus "colloquial extension" without
    // an @QuarkusTest runtime. The @Produces methods that wire CDI Instance<>
    // are not reachable from pure JUnit tests; full coverage of those happens
    // in the example integration test (examples/code-with-quarkus analog).
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.40".toBigDecimal()
            }
        }
    }
}

tasks.named("check") {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    finalizedBy(tasks.jacocoTestReport)
}

tasks.javadoc {
    (options as StandardJavadocDocletOptions).apply {
        addStringOption("Xdoclint:all,-missing", "-quiet")
        encoding = "UTF-8"
        charSet = "UTF-8"
    }
}

// Quarkus extensions use `enforcedPlatform` to align Quarkus extension versions
// across all transitive deps. This is the recommended pattern for Quarkus but
// Gradle 9.x marks it as a warning because it "leaks" to consumers — which is
// exactly what we want for a Quarkus extension. Suppress the validation.
tasks.withType<GenerateModuleMetadata>().configureEach {
    suppressedValidationErrors.add("enforced-platform")
}

checkstyle {
    toolVersion = "10.20.1"
    sourceSets = listOf(project.sourceSets.main.get())
}

// Generate META-INF/jandex.idx at processResources time so consuming Quarkus
// apps discover the @Singleton beans declared here. Without an index, the
// CDI build-time scan in Quarkus 3.x does not pick up classes from extension
// JARs and the consumer fails with UnsatisfiedResolutionException on
// NotificationFacade. We only index this project's own classes (not the
// transitive nova-notifications library) so we don't shadow beans produced
// by other jars.
val generateJandexIndex by tasks.registering {
    val outputDir = layout.buildDirectory.dir("generated/resources/jandex")
    inputs.files(sourceSets.main.get().output.classesDirs)
    inputs.files(configurations.runtimeClasspath)
    outputs.dir(outputDir)
    doLast {
        val classesDir = sourceSets.main.get().output.classesDirs.asPath
        val idxFile = outputDir.get().file("META-INF/jandex.idx").asFile
        idxFile.parentFile.mkdirs()

        // Build a classloader that includes this project's compiled classes
        // AND the runtime classpath (where jandex-core lives). The default
        // task classloader would not see Jandex on the runtime classpath.
        val urls = configurations.runtimeClasspath.get().files
            .map { it.toURI().toURL() }
            .toTypedArray()
        val classLoader = URLClassLoader(urls, javaClass.classLoader)
        val indexerCls = classLoader.loadClass("org.jboss.jandex.Indexer")
        val indexer = indexerCls.getDeclaredConstructor().newInstance()
        val fileCls = JFile::class.java
        val completeMethod = indexerCls.getMethod("complete")

        val classesFile = JFile(classesDir.toString())
        val streamMethod = indexerCls.getMethod("index", JInputStream::class.java)
        classesFile.walkTopDown().filter { it.name.endsWith(".class") }.forEach { f ->
            // Jandex 3.x dropped the index(File) overload in favor of
            // index(InputStream). Read the .class file and feed the stream.
            val stream = f.inputStream()
            try {
                streamMethod.invoke(indexer, stream)
            } finally {
                stream.close()
            }
        }
        val index = completeMethod.invoke(indexer)

        val writerCls = classLoader.loadClass("org.jboss.jandex.IndexWriter")
        // Jandex 3.x: IndexWriter(OutputStream) + write(Index).
        val writerCtor = writerCls.getConstructor(JOS::class.java)
        val writeMethod = writerCls.getMethod("write", index.javaClass)
        val fos = JFOS(idxFile)
        try {
            val writer = writerCtor.newInstance(fos)
            writeMethod.invoke(writer, index)
        } finally {
            fos.close()
        }
        logger.lifecycle("Wrote ${idxFile.absolutePath}")
    }
}

val processResources = tasks.named("processResources") {
    dependsOn(generateJandexIndex)
}

// Wire the generated META-INF/jandex.idx into the final JAR at exactly
// META-INF/jandex.idx (Quarkus build-time CDI scan looks there).
tasks.named<Copy>("processResources") {
    from(generateJandexIndex.map { layout.buildDirectory.dir("generated/resources/jandex") }) {
        include("META-INF/jandex.idx")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ahincho/nova-java-notifications-quarkus-extension")
            credentials {
                // Use findProperty first (works with -Pflag=val), fall back to env vars
                // (works in GitHub Actions where the publish step exports them).
                // System.getenv() alone failed silently when credentials were not
                // configured in time during the publish task evaluation, producing
                // a "ghost publish" (BUILD SUCCESSFUL but no JAR uploaded).
                username = findProperty("gpr.user") as String?
                    ?: System.getenv("GITHUB_ACTOR")
                    ?: "x-access-token"
                password = findProperty("gpr.key") as String?
                    ?: System.getenv("GITHUB_TOKEN")
                    ?: System.getenv("NOVA_RELEASE_PAT")
                    ?: System.getenv("NOVA_PACKAGES_READ_TOKEN")
                    ?: ""
            }
        }
    }
}
