package pe.edu.nova.java.starters.notifications.quarkus;

import io.smallrye.config.ConfigMapping;
import java.time.Duration;
import java.util.Optional;

/**
 * Quarkus configuration mapping for the notifications extension.
 *
 * <p>Maps the {@code nova.notifications.*} properties from
 * {@code application.properties}. Uses the same prefix as the Spring Boot
 * starter and Micronaut module, and follows the same convention as the
 * newer Nova starters (e.g. {@code nova-observability-spring-boot-starter}
 * uses {@code nova.observability.*}).
 *
 * <p>This interface is intentionally minimal: it only carries the
 * master {@code enabled} switch and the {@code resilience} defaults.
 * Each channel configuration (Email, Sms, Push, Slack) is exposed as a
 * SEPARATE top-level {@code @ConfigMapping} bean, and the
 * {@link NotificationsProducer} injects them independently. This is
 * the SmallRye 3.x idiom for nested config: the alternative (a single
 * outer {@code @ConfigMapping} with nested interface methods) silently
 * drops the nested property values because SmallRye treats the nested
 * interface as an opaque object instead of recursively binding into it.
 */
@ConfigMapping(prefix = "nova.notifications", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface NotificationsConfig {

    /** Master switch, default {@code true}. */
    Optional<Boolean> enabled();

    Resilience resilience();

    @ConfigMapping(prefix = "nova.notifications.email", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
    interface Email {
        /** "sendgrid" or "mailgun". */
        String provider();
        String apiKey();
        String defaultSender();
    }

    @ConfigMapping(prefix = "nova.notifications.sms", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
    interface Sms {
        default String provider() {
            return "twilio";
        }
        String accountSid();
        String authToken();
        String fromNumber();
    }

    @ConfigMapping(prefix = "nova.notifications.push", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
    interface Push {
        default String provider() {
            return "firebase";
        }
        String projectId();
        String serverKey();
    }

    @ConfigMapping(prefix = "nova.notifications.slack", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
    interface Slack {
        String defaultWebhookUrl();
    }

    @ConfigMapping(prefix = "nova.notifications.resilience", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
    interface Resilience {
        default int maxAttempts() {
            return 3;
        }
        default long initialBackoffMillis() {
            return 200;
        }
        default int circuitFailureThreshold() {
            return 5;
        }
        default long circuitOpenDurationSeconds() {
            return 30;
        }
        default int rateLimitPermitsPerSecond() {
            return 0;
        }
    }

    /** Hidden helper to convert a long millis to a {@link Duration}. */
    static Duration durationOfMillis(long millis) {
        return Duration.ofMillis(millis);
    }

    /** Hidden helper to convert a long seconds to a {@link Duration}. */
    static Duration durationOfSeconds(long seconds) {
        return Duration.ofSeconds(seconds);
    }
}