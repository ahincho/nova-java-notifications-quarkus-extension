# nova-java-notifications-quarkus-extension

Quarkus 3.33.2.1 LTS "colloquial" extension for the
[`nova-notifications`](../nova-java-notifications) library. When the
extension is on the classpath, a `NotificationFacade` bean is exposed
as a CDI `@Singleton` and ready to inject anywhere in the application.

This is the **Nivel 1 → Nivel 2** adapter for the Quarkus ecosystem in
Nova's meta-framework
(`docs/adrs/shared/ADR-001-arquitectura-meta-framework-cinco-niveles.md`).
The library is framework-agnostic; the extension is the only piece
that knows about Quarkus.

## Colloquial extension

This is a "colloquial" Quarkus extension: only CDI beans, **no
`@BuildStep`**, no deployment/runtime split, no Quarkus build-time
augmentation of the library. Quarkus auto-discovers the
`@Singleton` via its Jandex index.

The trade-off: a proper Quarkus extension (with `@BuildStep`) would
get build-time configuration validation, native-image integration
metadata, and the Quarkus dev-mode hot-reload. The Nova project
considers those features unnecessary for a notifications library and
chooses the much smaller surface area of a colloquial extension.

## Install

```kotlin
// build.gradle.kts
plugins {
    id("io.quarkus")
}

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("io.quarkus:quarkus-arc")
    implementation("io.quarkus:quarkus-config-yaml")
    implementation("pe.edu.nova.java.starters:nova-notifications-quarkus-extension:1.0.0")
}
```

```xml
<!-- pom.xml -->
<dependency>
    <groupId>pe.edu.nova.java.starters</groupId>
    <artifactId>nova-java-notifications-quarkus-extension</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick start

`application.properties`:

```properties
nova.notifications.enabled=true
nova.notifications.email.provider=sendgrid
nova.notifications.email.api-key=${SENDGRID_API_KEY}
nova.notifications.email.default-sender=no-reply@example.com
nova.notifications.resilience.max-attempts=3
```

`NotificationsResource.java`:

```java
@Path("/api/notifications")
@ApplicationScoped
public class NotificationsResource {

    @Inject
    NotificationFacade facade;

    @GET
    @Path("/email/welcome")
    @Produces(MediaType.APPLICATION_JSON)
    public NotificationResult welcome() {
        return facade.send(EmailNotification.builder()
                .from(new EmailAddress("no-reply@example.com"))
                .to(new EmailAddress("customer@example.com"))
                .subject(new Subject("Welcome"))
                .body(new MessageBody("Thanks for signing up to Nova."))
                .build());
    }
}
```

## Configuration reference

Each channel is a **separate top-level `@ConfigMapping` bean** in
SmallRye 3.x. The outer `NotificationsConfig` carries only the master
`enabled` switch and the resilience defaults; the channel configs are
siblings.

| Property | Type | Default | Description |
|---|---|---|---|
| `nova.notifications.enabled` | `Optional<Boolean>` | `true` | Master switch. When `false`, the producer returns a no-op `NotificationFacade` (every `send` returns `FAILED` with `ErrorCode.DISABLED`). |
| `nova.notifications.email.provider` | `String` | _(none)_ | `sendgrid` or `mailgun`. |
| `nova.notifications.email.api-key` | `String` | _(none)_ | Provider API key. |
| `nova.notifications.email.default-sender` | `String` | _(none)_ | Verified `EmailAddress`. |
| `nova.notifications.sms.provider` | `String` | `twilio` | |
| `nova.notifications.sms.account-sid` | `String` | _(none)_ | |
| `nova.notifications.sms.auth-token` | `String` | _(none)_ | |
| `nova.notifications.sms.from-number` | `String` | _(none)_ | |
| `nova.notifications.push.provider` | `String` | `firebase` | |
| `nova.notifications.push.project-id` | `String` | _(none)_ | |
| `nova.notifications.push.server-key` | `String` | _(none)_ | |
| `nova.notifications.slack.default-webhook-url` | `String` | _(none)_ | |
| `nova.notifications.resilience.max-attempts` | `int` | `3` | |
| `nova.notifications.resilience.initial-backoff-millis` | `long` | `200` | |
| `nova.notifications.resilience.circuit-failure-threshold` | `int` | `5` | |
| `nova.notifications.resilience.circuit-open-duration-seconds` | `long` | `30` | |
| `nova.notifications.resilience.rate-limit-permits-per-second` | `int` | `0` | `0` disables rate limiting. |

Each channel is independent. An empty or partial config for a channel
is treated as "channel not enabled" and the corresponding
`SendNotificationPort` is not registered.

## Disabling the library

```properties
nova.notifications.enabled=false
```

The producer returns a no-op `NotificationFacade` (every `send` returns
`FAILED` with `ErrorCode.DISABLED`). Consumers can still inject
`NotificationFacade` without a startup `ConfigurationException`; the
no-op contract is documented in
[`nova-java-notifications/README.md`](../nova-java-notifications/README.md).

## API reference

The extension does NOT add new public types beyond the producer and the
`@ConfigMapping` interfaces. The injected `NotificationFacade` and
`NotificationConfiguration` are pure-library types; see
[`nova-java-notifications/README.md`](../nova-java-notifications/README.md)
for the full API reference.

## Testing

```bash
./gradlew check
```

The extension ships with pure JUnit unit tests (8 tests). Per the Nova
convention for colloquial Quarkus extensions
(`docs/java/07-quarkus-analisis-adopcion.md`), the full
`@QuarkusTest` integration coverage lives in the example demo that
consumes the extension
([`examples/demo-notifications-quarkus`](../../examples/demo-notifications-quarkus)).

## Build

```bash
./gradlew build              # compile + test + jar
./gradlew publishToMavenLocal  # for the demo / other consumers
```

Quarkus types are at `implementation` scope (the Quarkus ARC runtime
must be on the classpath for the `@Singleton` beans to be wired). The
Quarkus 3.33.2.1 LTS BOM is the current pin.

## Versioning

- `1.0.0` — initial release aligned with `nova-notifications:1.0.0`.
- Property prefix: `nova.notifications.*` (Nova convention; older
  legacy starters still use `galaxy-training.*` — migration tracked
  in the meta-framework backlog).
- Java 25 toolchain (the workspace's standard pin).
- Quarkus 3.33.2.1 LTS (the last stable 3.33.x patch; the rest of
  Nova — e.g. `nova-java-api-standard-quarkus-extension` — still
  tracks 3.37.x, and a global migration is in the meta-framework
  backlog).

## Related

- [`nova-java-notifications`](../nova-java-notifications) — pure library.
- [`nova-java-notifications-spring-boot-starter`](../nova-java-notifications-spring-boot-starter) — Spring Boot auto-config.
- [`nova-java-notifications-micronaut-module`](../nova-java-notifications-micronaut-module) — Micronaut colloquial module.
- [`examples/demo-notifications-quarkus`](../../examples/demo-notifications-quarkus) — example app consuming this extension.

---

## AI Assistance Attribution

This work was created through human-AI collaboration. The human author
(Angel Eduardo Hincho Jove, `ahincho@unsa.edu.pe`, UNSA) retains full
responsibility for the final artifact.

**AI tools used**: GitHub Copilot (Claude Opus 4.8, Sonnet 5), MiniMax
(MiniMax-M3 via paid Token Plan), OpenCode (the interactive CLI
harness used to host the session), NotebookLM, Perplexity.
Methodology: OpenSpec spec-driven development.

**Important legal note**: this artifact is **not an "AI system"** under
Article 3(1) of Regulation (EU) 2024/1689 (the EU AI Act). Article 50
transparency obligations therefore do not directly apply. This
disclosure is made voluntarily, aligned with UNESCO Principle 6
(transparency and explainability) and the R-AI requirement of the
originating challenge.

The canonical, full AI-ATTRIBUTION.md (covering the entire Nova
Platform workspace) lives at the workspace root:
[`../../AI-ATTRIBUTION.md`](../../AI-ATTRIBUTION.md).
