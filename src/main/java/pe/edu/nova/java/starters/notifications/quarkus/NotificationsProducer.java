package pe.edu.nova.java.starters.notifications.quarkus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.edu.nova.java.libs.notifications.application.facade.NotificationFacade;
import pe.edu.nova.java.libs.notifications.application.port.out.NotificationEventPublisherPort;
import pe.edu.nova.java.libs.notifications.domain.vo.EmailAddress;
import pe.edu.nova.java.libs.notifications.domain.vo.SlackWebhookUrl;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.EmailConfiguration;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.EmailProvider;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.NotificationConfiguration;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.PushConfiguration;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.PushProvider;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.ResilienceConfiguration;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.SlackConfiguration;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.SmsConfiguration;
import pe.edu.nova.java.libs.notifications.infrastructure.configuration.SmsProvider;

/**
 * Quarkus producer that turns the {@link NotificationsConfig} into a real
 * {@link NotificationConfiguration} (pure library type) and exposes the
 * {@link NotificationFacade} as a {@code @Singleton} bean.
 *
 * <p>This is a "colloquial extension" (per
 * {@code docs/java/07-quarkus-analisis-adopcion.md}): only CDI beans, no
 * {@code @BuildStep}, no deployment/runtime split. Quarkus auto-discovers
 * the {@code @Singleton} via its Jandex index.
 */
@ApplicationScoped
public class NotificationsProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationsProducer.class);

    @Produces
    @Singleton
    public NotificationConfiguration notificationConfiguration(
            NotificationsConfig config,
            Instance<NotificationEventPublisherPort> customEventPublisher) {

        NotificationConfiguration.Builder builder = NotificationConfiguration.builder()
                .resilience(toResilience(config.resilience()));

        customEventPublisher.forEach(builder::eventPublisher);

        applyConfigToBuilder(config, builder);

        NotificationConfiguration configuration = builder.build();
        LOGGER.info("Nova Notifications (Quarkus) initialized. Email: {}, SMS: {}, Push: {}, Slack: {}",
                configuration.email().isPresent(),
                configuration.sms().isPresent(),
                configuration.push().isPresent(),
                configuration.slack().isPresent());
        return configuration;
    }

    /**
     * Visible for unit tests: the channel-mapping logic, decoupled from the
     * CDI {@code Instance} parameter which is hard to mock in pure JUnit.
     */
    void applyConfigToBuilder(NotificationsConfig config, NotificationConfiguration.Builder builder) {
        NotificationsConfig.Email email = config.email();
        if (email != null && allPresent(email.provider(), email.apiKey(), email.defaultSender())) {
            builder.email(EmailConfiguration.builder()
                    .provider(EmailProvider.valueOf(email.provider().toUpperCase()))
                    .apiKey(email.apiKey())
                    .defaultSender(new EmailAddress(email.defaultSender()))
                    .build());
        }
        config.sms().ifPresent(sms -> builder.sms(SmsConfiguration.builder()
                .provider(SmsProvider.valueOf(sms.provider().toUpperCase()))
                .accountSid(sms.accountSid())
                .authToken(sms.authToken())
                .fromNumber(sms.fromNumber())
                .build()));
        config.push().ifPresent(push -> builder.push(PushConfiguration.builder()
                .provider(PushProvider.valueOf(push.provider().toUpperCase()))
                .projectId(push.projectId())
                .serverKey(push.serverKey())
                .build()));
        config.slack().ifPresent(slack -> builder.slack(SlackConfiguration.builder()
                .defaultWebhookUrl(SlackWebhookUrl.of(slack.defaultWebhookUrl()))
                .build()));
    }

    @Produces
    @Singleton
    public NotificationFacade notificationFacade(NotificationConfiguration configuration) {
        return NotificationFacade.create(configuration);
    }

    private static ResilienceConfiguration toResilience(NotificationsConfig.Resilience r) {
        return new ResilienceConfiguration(
                r.maxAttempts(),
                NotificationsConfig.durationOfMillis(r.initialBackoffMillis()),
                r.circuitFailureThreshold(),
                NotificationsConfig.durationOfSeconds(r.circuitOpenDurationSeconds()),
                r.rateLimitPermitsPerSecond());
    }

    private static boolean allPresent(String... values) {
        for (String v : values) {
            if (v == null || v.isBlank()) {
                return false;
            }
        }
        return true;
    }
}
