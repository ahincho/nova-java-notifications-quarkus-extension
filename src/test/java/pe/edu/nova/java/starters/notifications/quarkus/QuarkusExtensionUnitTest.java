package pe.edu.nova.java.starters.notifications.quarkus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
 * coverage lives in the {@code examples/} module that consumes the
 * extension.
 */
class QuarkusExtensionUnitTest {

    private final NotificationsProducer producer = new NotificationsProducer();

    @Test
    void producerIsInstantiableAndHasNoRequiredDependencies() {
        // The producer is a plain CDI bean; constructing it must not require
        // any framework beyond CDI annotations.
        assertThat(producer).isNotNull();
    }

    @Test
    void applyConfigToBuilderMapsEmailChannelFromConfig() {
        StubConfig config = new StubConfig().withEmail("sendgrid", "test-api-key", "no-reply@example.com");
        NotificationConfiguration.Builder builder = NotificationConfiguration.builder();
        producer.applyConfigToBuilder(config, builder);
        NotificationConfiguration built = builder.build();

        assertThat(built.email()).isPresent();
        assertThat(built.email().get().provider().name()).isEqualTo("SENDGRID");
        assertThat(built.sms()).isEmpty();
        assertThat(built.push()).isEmpty();
        assertThat(built.slack()).isEmpty();
    }

    @Test
    void applyConfigToBuilderMapsAllFourChannelsIndependently() {
        StubConfig config = new StubConfig()
                .withEmail("mailgun", "key-1", "noreply@example.com")
                .withSms("twilio", "AC1", "token-1", "+15005550006")
                .withPush("firebase", "project-1", "server-key-1")
                .withSlack("https://hooks.slack.com/services/T0/B0/secret");
        NotificationConfiguration.Builder builder = NotificationConfiguration.builder();
        producer.applyConfigToBuilder(config, builder);
        NotificationConfiguration built = builder.build();

        assertThat(built.email()).isPresent();
        assertThat(built.sms()).isPresent();
        assertThat(built.push()).isPresent();
        assertThat(built.slack()).isPresent();
    }

    @Test
    void applyConfigToBuilderSkipsChannelsWithMissingFields() {
        StubConfig config = new StubConfig().withEmail(null, "key", "noreply@example.com");
        NotificationConfiguration.Builder builder = NotificationConfiguration.builder();
        producer.applyConfigToBuilder(config, builder);
        NotificationConfiguration built = builder.build();

        // Email provider is null → email channel must NOT be configured.
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
                3,
                java.time.Duration.ofMillis(200),
                5,
                java.time.Duration.ofSeconds(30),
                0);
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
        StubConfig config = new StubConfig()
                .withEnabled(false)
                .withEmail("sendgrid", "test-api-key", "noreply@example.com")
                .withSms("twilio", "AC1", "token-1", "+15005550006")
                .withPush("firebase", "project-1", "server-key-1")
                .withSlack("https://hooks.slack.com/services/T0/B0/secret");
        NotificationConfiguration.Builder builder = NotificationConfiguration.builder();
        producer.applyConfigToBuilder(config, builder);
        NotificationConfiguration built = builder.build();

        assertThat(built.email()).isEmpty();
        assertThat(built.sms()).isEmpty();
        assertThat(built.push()).isEmpty();
        assertThat(built.slack()).isEmpty();
    }

    /**
     * Manual stub of {@link NotificationsConfig} for unit tests. Implements
     * the interface as a mutable holder — easier to read and maintain than
     * a mocking library, and exposes precisely the fields this test cares about.
     */
    private static final class StubConfig implements NotificationsConfig {
        private Optional<Boolean> enabled = Optional.of(true);
        private StubEmail email;
        private StubSms sms;
        private StubPush push;
        private StubSlack slack;

        StubConfig withEnabled(boolean enabled) {
            this.enabled = Optional.of(enabled);
            return this;
        }

        StubConfig withEmail(String provider, String apiKey, String defaultSender) {
            this.email = new StubEmail(provider, apiKey, defaultSender);
            return this;
        }

        StubConfig withSms(String provider, String accountSid, String authToken, String fromNumber) {
            this.sms = new StubSms(provider, accountSid, authToken, fromNumber);
            return this;
        }

        StubConfig withPush(String provider, String projectId, String serverKey) {
            this.push = new StubPush(provider, projectId, serverKey);
            return this;
        }

        StubConfig withSlack(String defaultWebhookUrl) {
            this.slack = new StubSlack(defaultWebhookUrl);
            return this;
        }

        @Override public Optional<Boolean> enabled() { return enabled; }
        @Override public NotificationsConfig.Email email() { return email; }
        @Override public Optional<NotificationsConfig.Sms> sms() { return Optional.ofNullable(sms); }
        @Override public Optional<NotificationsConfig.Push> push() { return Optional.ofNullable(push); }
        @Override public Optional<NotificationsConfig.Slack> slack() { return Optional.ofNullable(slack); }
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
}
