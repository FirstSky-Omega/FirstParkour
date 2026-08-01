# Private server integrations

The public plugin contains only compatibility maintenance and general bug fixes.
Server-specific behavior should be kept in a separate private overlay.

An overlay implements `dev.efnilite.ip.api.ServerIntegration` and registers the
implementation in:

`META-INF/services/dev.efnilite.ip.api.ServerIntegration`

At startup, Infinite Parkour discovers providers with `ServiceLoader`, enables
them after public configuration is loaded, and disables them in reverse order.
One broken provider is logged without preventing the public modes from loading.

The release jar may be assembled by shading the public plugin and the private
provider together. Maven Shade's `ServicesResourceTransformer` must be enabled
so the provider registration survives packaging. Do not add private provider
classes or their service file to this public repository.

General fixes discovered while operating a private server should be applied to
the public core first whenever they are not server-specific. This keeps the
private overlay small and makes upstream updates straightforward.
