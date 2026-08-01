package dev.efnilite.ip.api;

import dev.efnilite.ip.IP;

/**
 * Optional integration extension point.
 *
 * <p>Providers are discovered through {@link java.util.ServiceLoader}.</p>
 */
public interface ServerIntegration {

    String id();

    void enable(IP plugin) throws Exception;

    void disable() throws Exception;
}
