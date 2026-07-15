package pe.edu.nova.java.starters.notifications.quarkus;

import io.smallrye.config.ConfigMapping;

/**
 * Quarkus configuration mapping for the Slack channel of the Nova
 * notifications extension.
 *
 * <p>Maps the {@code nova.notifications.slack.*} properties from
 * {@code application.properties}. See {@link EmailConfig} for the
 * rationale behind the top-level {@code @ConfigMapping} structure and
 * the {@code KEBAB_CASE} naming strategy.
 *
 * <pre>
 * nova.notifications.slack.default-webhook-url=https://hooks.slack.com/services/T0/B0/secret
 * </pre>
 */
@ConfigMapping(prefix = "nova.notifications.slack", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
public interface SlackConfig {

    String defaultWebhookUrl();
}