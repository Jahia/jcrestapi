/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.jcrestapi;

import org.jahia.services.securityfilter.PermissionService;

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
}
