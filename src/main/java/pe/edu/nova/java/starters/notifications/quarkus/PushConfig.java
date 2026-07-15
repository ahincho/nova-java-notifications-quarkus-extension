package pe.edu.nova.java.starters.notifications.quarkus;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Quarkus configuration mapping for the push-notifications channel of
 * the Nova notifications extension.
 *
 * <p>Maps the {@code nova.notifications.push.*} properties from
 * {@code application.properties}. See {@link EmailConfig} for the
 * rationale behind the top-level {@code @ConfigMapping} structure and
 * the {@code KEBAB_CASE} naming strategy.
 *
 * <pre>
 * nova.notifications.push.provider=firebase
 * nova.notifications.push.project-id=demo-project
 * nova.notifications.push.server-key=xxxxxxxx
 * </pre>
 */
@ConfigMapping(prefix = "nova.notifications.push", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
public interface PushConfig {

    @WithDefault("firebase")
    String provider();

    String projectId();

    String serverKey();
}