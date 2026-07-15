package pe.edu.nova.java.starters.notifications.quarkus;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Quarkus configuration mapping for the SMS channel of the Nova
 * notifications extension.
 *
 * <p>Maps the {@code nova.notifications.sms.*} properties from
 * {@code application.properties}. See {@link EmailConfig} for the
 * rationale behind the top-level {@code @ConfigMapping} structure and
 * the {@code KEBAB_CASE} naming strategy.
 *
 * <pre>
 * nova.notifications.sms.provider=twilio
 * nova.notifications.sms.account-sid=ACxxxxxxxx
 * nova.notifications.sms.auth-token=xxxxxxxx
 * nova.notifications.sms.from-number=+15555550100
 * </pre>
 */
@ConfigMapping(prefix = "nova.notifications.sms", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
public interface SmsConfig {

    @WithDefault("twilio")
    String provider();

    String accountSid();

    String authToken();

    String fromNumber();
}