/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 * <p>
 * http://www.jahia.com
 * <p>
 * Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
 * <p>
 * THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 * 1/GPL OR 2/JSEL
 * <p>
 * 1/ GPL
 * ==================================================================================
 * <p>
 * IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 * <p>
 * <p>
 * 2/ JSEL - Commercial and Supported Versions of the program
 * ===================================================================================
 * <p>
 * IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 * <p>
 * Alternatively, commercial and supported versions of the program - also known as
 * Enterprise Distributions - must be used in accordance with the terms and conditions
 * contained in a separate written agreement between you and Jahia Solutions Group SA.
 * <p>
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.jcrestapi;

import org.jahia.modules.securityfilter.PermissionService;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import java.util.Collections;
import java.util.Set;

/**
 * @author Christophe Laprun
 */
public final class SpringBeansAccess {
    private final static SpringBeansAccess INSTANCE = new SpringBeansAccess();
    private Repository repository;
    private boolean disableQuery = true;
    private Set<String> nodeTypesToSkip = Collections.emptySet();
    private PermissionService permissionService;
    private long restFileUploadMaxSize;

    private SpringBeansAccess() {
    }

    public static SpringBeansAccess getInstance() {
        return INSTANCE;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setQueryDisabled(String disableQuery) {
        this.disableQuery = Boolean.parseBoolean(disableQuery);
    }

    public boolean isQueryDisabled() {
        return disableQuery;
    }

    public void setNodeTypesToSkip(String nodeTypesToSkip) {
        this.nodeTypesToSkip = Utils.split(nodeTypesToSkip);
    }

    public Set<String> getNodeTypesToSkip() {
        return nodeTypesToSkip;
    }

    public PermissionService getPermissionService() {
        return permissionService;
    }

    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public boolean hasPermission(String api, Node node) throws RepositoryException {
        if (permissionService != null) {
            return permissionService.hasPermission(api, node);
        }
        return true;
    }

    public long getRestFileUploadMaxSize() {
        return restFileUploadMaxSize;
    }

    public void setRestFileUploadMaxSize(long restFileUploadMaxSize) {
        this.restFileUploadMaxSize = restFileUploadMaxSize;
    }
}
