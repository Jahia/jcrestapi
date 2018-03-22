/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2018 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.jcrestapi.accessors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.ws.rs.core.UriInfo;

import org.jahia.modules.jcrestapi.URIUtils;
import org.jahia.modules.jcrestapi.links.APIDecorator;
import org.jahia.modules.json.JSONNode;
import org.jahia.modules.json.JSONVersion;
import org.jahia.modules.json.JSONVersions;

/**
 * @author Christophe Laprun
 */
public class VersionElementAccessor extends ElementAccessor<JSONVersions<APIDecorator>, JSONVersion<APIDecorator>, JSONNode> {
    @Override
    protected JSONVersions getSubElementContainer(Node node, UriInfo context) throws RepositoryException {
        return getFactory().createVersions(getParentFrom(node), node);
    }

    @Override
    protected JSONVersion getSubElement(Node node, String subElement, UriInfo context) throws RepositoryException {
        final VersionHistory versionHistory = JSONVersions.getVersionHistoryFor(node);
        if (versionHistory != null) {
            final Version version = versionHistory.getVersion(subElement);
            return version != null ? getFactory().createVersion(node, version) : null;
        } else {
            return null;
        }
    }

    @Override
    protected void delete(Node node, String subElement) throws RepositoryException {
        throw new UnsupportedOperationException("Cannot delete versions");
    }

    @Override
    protected CreateOrUpdateResult<JSONVersion<APIDecorator>> createOrUpdate(Node node, String subElement, JSONNode childData) throws RepositoryException {
        throw new UnsupportedOperationException("Cannot create or update versions");
    }

    @Override
    protected String getSeeOtherURIAsString(Node node) {
        return URIUtils.getURIForVersions(node);
    }
}
