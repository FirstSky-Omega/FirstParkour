package dev.efnilite.ip.api;

import dev.efnilite.ip.IP;

/**
 * Optional server-specific extension point.
 *
 * <p>The public distribution ships without providers. A private distribution can shade
 * providers into the jar and register them through {@link java.util.ServiceLoader}.</p>
 */
public interface ServerIntegration {

    String id();

    void enable(IP plugin) throws Exception;

    void disable() throws Exception;
}
