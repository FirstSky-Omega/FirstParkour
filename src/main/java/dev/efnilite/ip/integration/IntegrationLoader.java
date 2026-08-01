package dev.efnilite.ip.integration;

import dev.efnilite.ip.IP;
import dev.efnilite.ip.api.ServerIntegration;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.logging.Level;

public final class IntegrationLoader {

    private IntegrationLoader() {
    }

    public static List<ServerIntegration> enable(IP plugin) {
        List<ServerIntegration> enabled = new ArrayList<>();
        try {
            for (ServerIntegration integration : ServiceLoader.load(ServerIntegration.class, plugin.getClass().getClassLoader())) {
                try {
                    integration.enable(plugin);
                    enabled.add(integration);
                    plugin.getLogger().info("Enabled server integration: " + integration.id());
                } catch (Exception exception) {
                    plugin.getLogger().log(Level.SEVERE, "Could not enable server integration " + integration.id(), exception);
                }
            }
        } catch (ServiceConfigurationError error) {
            plugin.getLogger().log(Level.SEVERE, "Could not discover server integrations", error);
        }
        return List.copyOf(enabled);
    }

    public static void disable(IP plugin, List<ServerIntegration> integrations) {
        for (int index = integrations.size() - 1; index >= 0; index--) {
            ServerIntegration integration = integrations.get(index);
            try {
                integration.disable();
            } catch (Exception exception) {
                plugin.getLogger().log(Level.SEVERE, "Could not disable server integration " + integration.id(), exception);
            }
        }
    }
}
