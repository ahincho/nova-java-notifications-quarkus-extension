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

    @Test
    void producerIsInstantiableAndHasNoRequiredDependencies() {
        NotificationsProducer producer = new NotificationsProducer();
        assertThat(producer).isNotNull();
    }

    @Test
    void applyConfigToBuilderMapsEmailChannelFromConfig() {
        NotificationsProducer producer = newProducer(true,
                new EmailConfig() {
                    public String provider() { return "sendgrid"; }
                    public String apiKey() { return "test-api-key"; }
                    public String defaultSender() { return "no-reply@example.com"; }
                },
                null, null, null);
        NotificationConfiguration.Builder builder = NotificationConfiguration.builder();
        producer.applyConfigToBuilder(builder);
        NotificationConfiguration built = builder.build();

        assertThat(built.email()).isPresent();
        assertThat(built.email().get().provider().name()).isEqualTo("SENDGRID");
        assertThat(built.sms()).isEmpty();
        assertThat(built.push()).isEmpty();
        assertThat(built.slack()).isEmpty();
    }

    @Test
    void applyConfigToBuilderMapsAllFourChannelsIndependently() {
        NotificationsProducer producer = newProducer(true,
                new EmailConfig() {
                    public String provider() { return "mailgun"; }
                    public String apiKey() { return "key-1"; }
                    public String defaultSender() { return "noreply@example.com"; }
                },
                new SmsConfig() {
                    public String provider() { return "twilio"; }
                    public String accountSid() { return "AC1"; }
                    public String authToken() { return "token-1"; }
                    public String fromNumber() { return "+15005550006"; }
                },
                new PushConfig() {
                    public String provider() { return "firebase"; }
                    public String projectId() { return "project-1"; }
                    public String serverKey() { return "server-key-1"; }
                },
                new SlackConfig() {
                    public String defaultWebhookUrl() { return "https://hooks.slack.com/services/T0/B0/secret"; }
                });
        NotificationConfiguration.Builder builder = NotificationConfiguration.builder();
        producer.applyConfigToBuilder(builder);
        NotificationConfiguration built = builder.build();

        assertThat(built.email()).isPresent();
        assertThat(built.sms()).isPresent();
        assertThat(built.push()).isPresent();
        assertThat(built.slack()).isPresent();
    }

    @Test
    void applyConfigToBuilderSkipsChannelsWithMissingFields() {
        NotificationsProducer producer = newProducer(true,
                new EmailConfig() {
                    public String provider() { return null; }
                    public String apiKey() { return "key"; }
                    public String defaultSender() { return "noreply@example.com"; }
                },
                null, null, null);
        NotificationConfiguration.Builder builder = NotificationConfiguration.builder();
        producer.applyConfigToBuilder(builder);
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
        NotificationsProducer producer = newProducer(false,
                new EmailConfig() {
                    public String provider() { return "sendgrid"; }
                    public String apiKey() { return "test-api-key"; }
                    public String defaultSender() { return "no-reply@example.com"; }
                },
                new SmsConfig() {
                    public String provider() { return "twilio"; }
                    public String accountSid() { return "AC1"; }
                    public String authToken() { return "token-1"; }
                    public String fromNumber() { return "+15005550006"; }
                },
                new PushConfig() {
                    public String provider() { return "firebase"; }
                    public String projectId() { return "project-1"; }
                    public String serverKey() { return "server-key-1"; }
                },
                new SlackConfig() {
                    public String defaultWebhookUrl() { return "https://hooks.slack.com/services/T0/B0/secret"; }
                });
        NotificationConfiguration.Builder builder = NotificationConfiguration.builder();
        producer.applyConfigToBuilder(builder);
        NotificationConfiguration built = builder.build();

        assertThat(built.email()).isEmpty();
        assertThat(built.sms()).isEmpty();
        assertThat(built.push()).isEmpty();
        assertThat(built.slack()).isEmpty();
    }

    /** Build a producer with stubbed channel configs for testing. */
    private static NotificationsProducer newProducer(boolean enabled, EmailConfig email,
                                                      SmsConfig sms, PushConfig push, SlackConfig slack) {
        NotificationsProducer producer = new NotificationsProducer();
        producer.config = () -> Optional.of(enabled);
        producer.emailConfig = email == null ? new SingleElementInstance<>(null) : new SingleElementInstance<>(email);
        producer.smsConfig = sms == null ? new SingleElementInstance<>(null) : new SingleElementInstance<>(sms);
        producer.pushConfig = push == null ? new SingleElementInstance<>(null) : new SingleElementInstance<>(push);
        producer.slackConfig = slack == null ? new SingleElementInstance<>(null) : new SingleElementInstance<>(slack);
        producer.resilienceConfig = new SingleElementInstance<>(null);
        producer.customEventPublisher = new SingleElementInstance<>(null);
        return producer;
    }

    /**
     * CDI {@link Instance} adapter for unit tests: presents a single
     * element (or none) without depending on Quarkus's full ARC runtime.
     */
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