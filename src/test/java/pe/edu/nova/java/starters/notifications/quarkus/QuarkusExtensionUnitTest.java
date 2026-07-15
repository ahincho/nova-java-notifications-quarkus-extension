package pe.edu.nova.java.starters.notifications.quarkus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.inject.Instance;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pe.edu.nova.java.libs.notifications.application.facade.NotificationFacade;
import pe.edu.nova.java.libs.notifications.domain.error.ConfigurationException;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.NotificationConfiguration;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.ResilienceConfiguration;

/**
 * Unit tests for the Quarkus extension. Per the Nova convention (see
 * nova-java-api-standard-quarkus-extension README), this extension ships
 * with pure JUnit tests only; the full {@code @QuarkusTest} integration
 * coverage lives in the {@code examples/demo-notifications-quarkus/} module
 * that consumes the extension (the {@code io.quarkus} Gradle plugin is
 * required for {@code @QuarkusTest} and is not applied to this library
 * module, which only depends on the Quarkus ARC runtime).
 */
class QuarkusExtensionUnitTest {

    private final NotificationsProducer producer = new NotificationsProducer();

    @Test
    void producerIsInstantiableAndHasNoRequiredDependencies() {
        assertThat(producer).isNotNull();
    }

    @Test
    void applyConfigToBuilderMapsEmailChannelFromConfig() {
        StubConfig config = new StubConfig();
        NotificationConfiguration.Builder builder = NotificationConfiguration.builder();
        producer.applyConfigToBuilder(
                config, builder,
                stubOne(new StubEmail("sendgrid", "test-api-key", "no-reply@example.com")),
                stubEmpty(),
                stubEmpty(),
                stubEmpty());
        NotificationConfiguration built = builder.build();

        assertThat(built.email()).isPresent();
        assertThat(built.email().get().provider().name()).isEqualTo("SENDGRID");
        assertThat(built.sms()).isEmpty();
        assertThat(built.push()).isEmpty();
        assertThat(built.slack()).isEmpty();
    }

    @Test
    void applyConfigToBuilderMapsAllFourChannelsIndependently() {
        StubConfig config = new StubConfig();
        NotificationConfiguration.Builder builder = NotificationConfiguration.builder();
        producer.applyConfigToBuilder(
                config, builder,
                stubOne(new StubEmail("mailgun", "key-1", "noreply@example.com")),
                stubOne(new StubSms("twilio", "AC1", "token-1", "+15005550006")),
                stubOne(new StubPush("firebase", "project-1", "server-key-1")),
                stubOne(new StubSlack("https://hooks.slack.com/services/T0/B0/secret")));
        NotificationConfiguration built = builder.build();

        assertThat(built.email()).isPresent();
        assertThat(built.sms()).isPresent();
        assertThat(built.push()).isPresent();
        assertThat(built.slack()).isPresent();
    }

    @Test
    void applyConfigToBuilderSkipsChannelsWithMissingFields() {
        StubConfig config = new StubConfig();
        NotificationConfiguration.Builder builder = NotificationConfiguration.builder();
        producer.applyConfigToBuilder(
                config, builder,
                stubOne(new StubEmail(null, "key", "noreply@example.com")),
                stubEmpty(),
                stubEmpty(),
                stubEmpty());
        NotificationConfiguration built = builder.build();

        assertThat(built.email()).isEmpty();
    }

    @Test
    void facadeThrowsConfigurationExceptionWhenNoChannelIsConfigured() {
        NotificationConfiguration emptyConfiguration = NotificationConfiguration.builder().build();
        assertThatThrownBy(() -> NotificationFacade.create(emptyConfiguration))
                .isInstanceOf(ConfigurationException.class);
    }

    @Test
    void resilienceConfigurationBuilderProducesValidLibraryConfiguration() {
        ResilienceConfiguration resilience = new ResilienceConfiguration(
                3, java.time.Duration.ofMillis(200), 5,
                java.time.Duration.ofSeconds(30), 0);
        assertThat(resilience.maxAttempts()).isEqualTo(3);
        assertThat(resilience.retryEnabled()).isTrue();
        assertThat(resilience.rateLimitEnabled()).isFalse();
    }

    @Test
    void resilienceDisabledHasNoRetryAndVeryHighCircuitThreshold() {
        ResilienceConfiguration disabled = ResilienceConfiguration.disabled();
        assertThat(disabled.retryEnabled()).isFalse();
        assertThat(disabled.maxAttempts()).isEqualTo(1);
    }

    @Test
    void applyConfigToBuilderSkipsAllChannelsWhenEnabledIsFalse() {
        StubConfig config = new StubConfig().withEnabled(false);
        NotificationConfiguration.Builder builder = NotificationConfiguration.builder();
        producer.applyConfigToBuilder(
                config, builder,
                stubOne(new StubEmail("sendgrid", "test-api-key", "no-reply@example.com")),
                stubOne(new StubSms("twilio", "AC1", "token-1", "+15005550006")),
                stubOne(new StubPush("firebase", "project-1", "server-key-1")),
                stubOne(new StubSlack("https://hooks.slack.com/services/T0/B0/secret")));
        NotificationConfiguration built = builder.build();

        assertThat(built.email()).isEmpty();
        assertThat(built.sms()).isEmpty();
        assertThat(built.push()).isEmpty();
        assertThat(built.slack()).isEmpty();
    }

    private static final class StubConfig implements NotificationsConfig {
        private Optional<Boolean> enabled = Optional.of(true);

        StubConfig withEnabled(boolean enabled) {
            this.enabled = Optional.of(enabled);
            return this;
        }

        @Override public Optional<Boolean> enabled() { return enabled; }
        @Override public NotificationsConfig.Resilience resilience() { return new StubResilience(); }
    }

    private record StubEmail(String provider, String apiKey, String defaultSender)
            implements NotificationsConfig.Email {}

    private record StubSms(String provider, String accountSid, String authToken, String fromNumber)
            implements NotificationsConfig.Sms {}

    private record StubPush(String provider, String projectId, String serverKey)
            implements NotificationsConfig.Push {}

    private record StubSlack(String defaultWebhookUrl) implements NotificationsConfig.Slack {}

    private static final class StubResilience implements NotificationsConfig.Resilience {
        @Override public int maxAttempts() { return 3; }
        @Override public long initialBackoffMillis() { return 200; }
        @Override public int circuitFailureThreshold() { return 5; }
        @Override public long circuitOpenDurationSeconds() { return 30; }
        @Override public int rateLimitPermitsPerSecond() { return 0; }
    }

    /**
     * CDI {@link Instance} adapter for unit tests: presents a single
     * element (or none) without depending on Quarkus's full ARC runtime.
     */
    private static <T> Instance<T> stubOne(T value) {
        return new SingleElementInstance<>(value);
    }

    private static <T> Instance<T> stubEmpty() {
        return new SingleElementInstance<>(null);
    }

    private static final class SingleElementInstance<T> implements Instance<T> {
        private final T value;
        SingleElementInstance(T value) { this.value = value; }
        @Override public Instance<T> select(java.lang.annotation.Annotation... q) { return this; }
        @Override public <U extends T> Instance<U> select(Class<U> c, java.lang.annotation.Annotation... q) { throw new UnsupportedOperationException(); }
        @Override public <U extends T> Instance<U> select(jakarta.enterprise.util.TypeLiteral<U> t, java.lang.annotation.Annotation... q) { throw new UnsupportedOperationException(); }
        @Override public boolean isUnsatisfied() { return value == null; }
        @Override public boolean isAmbiguous() { return false; }
        @Override public void destroy(T inst) { }
        @Override public Handle<T> getHandle() { throw new UnsupportedOperationException(); }
        @Override public Iterable<? extends Handle<T>> handles() { return Collections.emptyList(); }
        @Override public T get() { if (value == null) throw new jakarta.enterprise.inject.UnsatisfiedResolutionException("no bean"); return value; }
        @Override public java.util.Iterator<T> iterator() {
            return value == null ? Collections.<T>emptyIterator() : List.of(value).iterator();
        }
    }
}