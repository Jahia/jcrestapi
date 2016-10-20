package org.jahia.modules.jcrestapi.api;

/**
 * Service to handle prepared queries
 *
 * @author toto
 */
public interface PreparedQueryService {
    /**
     * Get a query based on its name
     * @param name
     * @return
     */
    public PreparedQuery getQuery(String name);

    /**
     * Unregister a PreparedQuery object
     * @param preparedQuery
     */
    public void removeQuery(PreparedQuery preparedQuery);

    /**
     * Register a PreparedQuery object
     * @param preparedQuery
     */
    public void addQuery(PreparedQuery preparedQuery);

}
