package pe.edu.nova.java.starters.notifications.quarkus;

import io.smallrye.config.ConfigMapping;
import java.util.Optional;

/**
 * Quarkus configuration mapping for the master switch of the Nova
 * notifications extension.
 *
 * <p>This interface is intentionally minimal: it only carries the
 * master {@code enabled} flag. Each channel (email, sms, push, slack)
 * has its OWN top-level {@code @ConfigMapping} interface
 * ({@link EmailConfig}, {@link SmsConfig}, {@link PushConfig},
 * {@link SlackConfig}) which is the SmallRye 3.x idiom for nested
 * config. The alternative — a single outer {@code @ConfigMapping} with
 * nested interfaces — silently drops the nested property values
 * because SmallRye treats the nested interface as an opaque object
 * instead of recursively binding into it.
 *
 * <p>Map of properties:
 *
 * <pre>
 * nova.notifications.enabled=true
 * nova.notifications.resilience.max-attempts=3
 * </pre>
 *
 * <p>Resilience is exposed as a top-level config too:
 * {@code nova.notifications.resilience.*} is mapped by the
 * ResilienceConfig top-level interface.
 */
@ConfigMapping(prefix = "nova.notifications", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
public interface NotificationsConfig {

    /** Master switch, default {@code true}. */
    Optional<Boolean> enabled();
}