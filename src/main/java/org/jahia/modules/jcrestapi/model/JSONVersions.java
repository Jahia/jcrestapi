/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2013 Jahia Solutions Group SA. All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 * As a special exception to the terms and conditions of version 2.0 of
 * the GPL (or any later version), you may redistribute this Program in connection
 * with Free/Libre and Open Source Software ("FLOSS") applications as described
 * in Jahia's FLOSS exception. You should have received a copy of the text
 * describing the FLOSS exception, and it is also available here:
 * http://www.jahia.com/license
 *
 * Commercial and Supported Versions of the program (dual licensing):
 * alternatively, commercial and supported versions of the program may be used
 * in accordance with the terms and conditions contained in a separate
 * written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.jcrestapi.model;

import org.jahia.api.Constants;
import org.jahia.modules.jcrestapi.API;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Christophe Laprun
 */
@XmlRootElement
public class JSONVersions extends JSONSubElementContainer {

    private Map<String, JSONVersion> versions;

    public JSONVersions(JSONNode parent, Node node) throws RepositoryException {
        super(parent, API.VERSIONS);

        if (isNodeVersionable(node)) {
            final Session session = API.getCurrentSession();
            if (session != null) {
                final VersionManager versionManager = session.getWorkspace().getVersionManager();
                final String path = node.getPath();

                final VersionHistory versionHistory = versionManager.getVersionHistory(path);
                final VersionIterator allVersions = versionHistory.getAllVersions();
                versions = new LinkedHashMap<String, JSONVersion>((int) allVersions.getSize());
                while (allVersions.hasNext()) {
                    final Version version = allVersions.nextVersion();
                    versions.put(version.getName(), new JSONVersion(node, version));
                }
            }
        } else {
            versions = Collections.emptyMap();
        }
    }

    public static boolean isNodeVersionable(Node node) throws RepositoryException {
        return node.isNodeType(Constants.MIX_VERSIONABLE) || node.isNodeType(Constants.MIX_SIMPLEVERSIONABLE);
    }

    public Map<String, JSONVersion> getVersions() {
        return versions;
    }
}
