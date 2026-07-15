package pe.edu.nova.java.starters.notifications.quarkus;

import io.smallrye.config.ConfigMapping;

/**
 * Quarkus configuration mapping for the email channel of the Nova
 * notifications extension.
 *
 * <p>Maps the {@code nova.notifications.email.*} properties from
 * {@code application.properties}:
 *
 * <pre>
 * nova.notifications.email.provider=sendgrid
 * nova.notifications.email.api-key=SG.demo
 * nova.notifications.email.default-sender=no-reply@example.com
 * </pre>
 *
 * <p>This is a TOP-LEVEL {@code @ConfigMapping} interface on purpose.
 * SmallRye 3.x (shipped with Quarkus 3.33.x) does not recursively bind
 * into nested {@code @ConfigMapping} interfaces declared inside a
 * parent {@code @ConfigMapping}: the parent treats the nested interface
 * as an opaque object and the nested property values are silently
 * dropped. Keeping each channel as its own top-level interface is the
 * documented SmallRye idiom and is the only structure that survives a
 * full {@code @QuarkusTest} bootstrap (which is when SmallRye applies
 * its build-time bindings).
 *
 * <p>{@code namingStrategy = KEBAB_CASE} makes property names in
 * {@code application.properties} follow the kebab-case convention
 * (e.g. {@code api-key}, {@code default-sender}) which matches the
 * Spring Boot starter and Micronaut module for cross-framework
 * consistency.
 */
@ConfigMapping(prefix = "nova.notifications.email", namingStrategy = ConfigMapping.NamingStrategy.KEBAB_CASE)
public interface EmailConfig {

    /** "sendgrid" or "mailgun". */
    String provider();

    String apiKey();

    String defaultSender();
}