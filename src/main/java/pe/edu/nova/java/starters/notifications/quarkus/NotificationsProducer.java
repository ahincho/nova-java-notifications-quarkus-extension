package pe.edu.nova.java.starters.notifications.quarkus;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
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

import java.time.Duration;

/**
 * Quarkus producer that turns the channel-specific
 * {@code @ConfigMapping} beans ({@link EmailConfig}, {@link SmsConfig},
 * {@link PushConfig}, {@link SlackConfig}, {@link ResilienceConfig})
 * plus the master {@link NotificationsConfig} switch into a real
 * {@link NotificationConfiguration} (pure library type) and exposes
 * the {@link NotificationFacade} as a {@code @Singleton} bean.
 *
 * <p>This is a "colloquial extension" (per
 * {@code docs/java/07-quarkus-analisis-adopcion.md}): only CDI beans,
 * no {@code @BuildStep}, no deployment/runtime split. Quarkus
 * auto-discovers the {@code @Singleton} via its Jandex index.
 *
 * <p>Each channel config is a separate top-level bean because SmallRye
 * 3.x does not recursively bind into nested {@code @ConfigMapping}
 * interfaces — see {@link NotificationsConfig} for the rationale.
 */
@ApplicationScoped
public class NotificationsProducer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationsProducer.class);

    /** Master switch. Package-private so unit tests can inject a stub. */
    @Inject
    NotificationsConfig config;

    /** Channel configs. Package-private so unit tests can inject stubs. */
    @Inject
    Instance<EmailConfig> emailConfig;

    @Inject
    Instance<SmsConfig> smsConfig;

    @Inject
    Instance<PushConfig> pushConfig;

    @Inject
    Instance<SlackConfig> slackConfig;

    @Inject
    Instance<ResilienceConfig> resilienceConfig;

    @Inject
    Instance<NotificationEventPublisherPort> customEventPublisher;

    @Produces
    @Singleton
    public NotificationConfiguration notificationConfiguration() {
        NotificationConfiguration.Builder builder = NotificationConfiguration.builder();

        resilienceConfig.forEach(r -> builder.resilience(toResilience(r)));

        customEventPublisher.forEach(builder::eventPublisher);

        if (!config.enabled().orElse(true)) {
            // Library is disabled at the starter level: skip all channel
            // assembly. The resilience config (set above) is kept. The
            // corresponding {@link #notificationFacade} method returns a
            // no-op facade so consumers can still inject
            // {@code NotificationFacade} without a startup
            // {@code ConfigurationException}.
            return builder.build();
        }

        applyConfigToBuilder(builder);

        NotificationConfiguration configuration = builder.build();
        LOGGER.info("Nova Notifications (Quarkus) initialized. Email: {}, SMS: {}, Push: {}, Slack: {}",
                configuration.email().isPresent(),
                configuration.sms().isPresent(),
                configuration.push().isPresent(),
                configuration.slack().isPresent());
        return configuration;
    }

    /**
     * Visible for unit tests: the channel-mapping logic, decoupled from
     * the {@code @Inject} fields by going through public accessor
     * methods that return a fresh builder view. Tests construct a
     * {@code NotificationsProducer} with explicit channel config values.
     */
    void applyConfigToBuilder(NotificationConfiguration.Builder builder) {
        emailConfig.forEach(email -> {
            if (allPresent(email.provider(), email.apiKey(), email.defaultSender())) {
                builder.email(EmailConfiguration.builder()
                        .provider(EmailProvider.valueOf(email.provider().toUpperCase()))
                        .apiKey(email.apiKey())
                        .defaultSender(new EmailAddress(email.defaultSender()))
                        .build());
            }
        });
        smsConfig.forEach(sms -> builder.sms(SmsConfiguration.builder()
                .provider(SmsProvider.valueOf(sms.provider().toUpperCase()))
                .accountSid(sms.accountSid())
                .authToken(sms.authToken())
                .fromNumber(sms.fromNumber())
                .build()));
        pushConfig.forEach(push -> builder.push(PushConfiguration.builder()
                .provider(PushProvider.valueOf(push.provider().toUpperCase()))
                .projectId(push.projectId())
                .serverKey(push.serverKey())
                .build()));
        slackConfig.forEach(slack -> builder.slack(SlackConfiguration.builder()
                .defaultWebhookUrl(SlackWebhookUrl.of(slack.defaultWebhookUrl()))
                .build()));
    }

    @Produces
    @Singleton
    public NotificationFacade notificationFacade(NotificationConfiguration configuration) {
        if (!config.enabled().orElse(true)) {
            LOGGER.warn("Nova Notifications (Quarkus) is disabled via nova.notifications.enabled=false; "
                    + "producing a no-op facade (every send returns FAILED with ErrorCode.DISABLED).");
            return NotificationFacade.createDisabled();
        }
        return NotificationFacade.create(configuration);
    }

    private static ResilienceConfiguration toResilience(ResilienceConfig r) {
        return new ResilienceConfiguration(
                r.maxAttempts(),
                Duration.ofMillis(r.initialBackoffMillis()),
                r.circuitFailureThreshold(),
                Duration.ofSeconds(r.circuitOpenDurationSeconds()),
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