package org.jahia.modules.jcrestapi;

import org.jahia.osgi.BundleUtils;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * A filter that logs deprecation warnings for deprecated REST API endpoints.
 */
@Provider
public class JCRRestAPIDeprecationFilter implements ContainerResponseFilter {

    public static final Logger logger = LoggerFactory.getLogger(JCRRestAPIDeprecationFilter.class);
    private static final Map<String, Long> loggedPaths = new ConcurrentHashMap<>();

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        // Always send deprecation header:
        responseContext.getHeaders().add("Deprecation", true);

        // Log deprecation warning if needed:
        ConfigurationAdmin configAdmin = BundleUtils.getOsgiService(ConfigurationAdmin.class, null);
        long logAgainThreshold = TimeUnit.HOURS.toMillis(24); // default re-log every 24 hours

        if (configAdmin != null) {
            Configuration configuration = configAdmin.getConfiguration("org.jahia.modules.jcrestapi");
            if (configuration.getProperties() != null) {
                if (Boolean.parseBoolean((String) configuration.getProperties().get("deprecation.warning.disabled"))) {
                    // deprecation warning is disabled
                    return;
                }

                String logAgainThresholdStr = (String) configuration.getProperties().get("deprecation.warning.logAgainThreshold.hour");
                if (logAgainThresholdStr != null) {
                    try {
                        logAgainThreshold = TimeUnit.HOURS.toMillis(Long.parseLong(logAgainThresholdStr));
                    } catch (NumberFormatException e) {
                        logger.error("Invalid number format for deprecation.warning.logAgainThreshold.hour, fallback to 24", e);
                    }
                }
            }
        }

        UriInfo uriInfo = requestContext.getUriInfo();
        String method = requestContext.getMethod();
        String basePath = uriInfo.getBaseUri().getPath();
        String apiPath = uriInfo.getPath();
        String key = method + " " + apiPath;
        long now = Instant.now().toEpochMilli();

        Long lastLogged = loggedPaths.get(key);
        if (lastLogged == null || now - lastLogged >= logAgainThreshold) {
            logger.warn("JCR REST API is deprecated. Received a {} request to endpoint: [{}]. " +
                    "Please refer to the GraphQL API for supported alternatives.", method, basePath + apiPath);
            loggedPaths.put(key, now);
        }
    }
}
