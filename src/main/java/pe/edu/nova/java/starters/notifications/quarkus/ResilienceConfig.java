package pe.edu.nova.java.starters.notifications.quarkus;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Quarkus configuration mapping for the resilience defaults of the
 * Nova notifications extension.
 *
 * <p>Maps the {@code nova.notifications.resilience.*} properties from
 * {@code application.properties}. See {@link EmailConfig} for the
 * rationale behind the top-level {@code @ConfigMapping} structure and
 * the {@code KEBAB_CASE} naming strategy.
 *
 * <pre>
 * nova.notifications.resilience.max-attempts=3
 * nova.notifications.resilience.initial-backoff-millis=200
 * nova.notifications.resilience.circuit-failure-threshold=5
 * nova.notifications.resilience.circuit-open-duration-seconds=30
 * nova.notifications.resilience.rate-limit-permits-per-second=0
 * </pre>
 */
@ConfigMapping(prefix = "nova.notifications.resilience", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
public interface ResilienceConfig {

    @WithDefault("3")
    int maxAttempts();

    @WithDefault("200")
    long initialBackoffMillis();

    @WithDefault("5")
    int circuitFailureThreshold();

    @WithDefault("30")
    long circuitOpenDurationSeconds();

    @WithDefault("0")
    int rateLimitPermitsPerSecond();
}