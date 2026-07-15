package pe.edu.nova.java.starters.notifications.quarkus;

import io.smallrye.config.ConfigMapping;
import java.time.Duration;
import java.util.Optional;

/**
 * Quarkus configuration mapping for the notifications extension.
 *
 * <p>Maps the {@code galaxy-training.notifications.*} properties in
 * {@code application.properties} (kept consistent with the Spring Boot
 * starter; see {@code NotificationsProperties} in the SB module for the
 * shared naming rationale).
 */
@ConfigMapping(prefix = "galaxy-training.notifications", namingStrategy = ConfigMapping.NamingStrategy.VERBATIM)
public interface NotificationsConfig {

    /** Master switch, default {@code true}. */
    Optional<Boolean> enabled();

    Email email();

    Optional<Sms> sms();

    Optional<Push> push();

    Optional<Slack> slack();

    Resilience resilience();

    interface Email {
        /** "sendgrid" or "mailgun". */
        String provider();
        String apiKey();
        String defaultSender();
    }

    interface Sms {
        default String provider() {
            return "twilio";
        }
        String accountSid();
        String authToken();
        String fromNumber();
    }

    interface Push {
        default String provider() {
            return "firebase";
        }
        String projectId();
        String serverKey();
    }

    interface Slack {
        String defaultWebhookUrl();
    }

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

    /** Hidden helper to convert a long millis to a {@link Duration} (used by the recorder). */
    static Duration durationOfMillis(long millis) {
        return Duration.ofMillis(millis);
    }

    /** Hidden helper to convert a long seconds to a {@link Duration}. */
    static Duration durationOfSeconds(long seconds) {
        return Duration.ofSeconds(seconds);
    }
}
