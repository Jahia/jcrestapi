/**
 * This file is part of Jahia, next-generation open source CMS:
 * Jahia's next-generation, open source CMS stems from a widely acknowledged vision
 * of enterprise application convergence - web, search, document, social and portal -
 * unified by the simplicity of web content management.
 *
 * For more information, please visit http://www.jahia.com.
 *
 * Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.jcrestapi.accessors;

import org.jahia.modules.jcrestapi.URIUtils;
import org.jahia.modules.jcrestapi.model.JSONChildren;
import org.jahia.modules.jcrestapi.model.JSONNode;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

/**
 * @author Christophe Laprun
 */
public class ChildrenElementAccessor extends ElementAccessor<JSONChildren, JSONNode, JSONNode> {
    @Override
    protected JSONChildren getSubElementContainer(Node node) throws RepositoryException {
        return new JSONChildren(getParentFrom(node), node);
    }

    @Override
    protected JSONNode getSubElement(Node node, String subElement) throws RepositoryException {
        return new JSONNode(node.getNode(subElement), 1);
    }

    @Override
    protected void delete(Node node, String subElement) throws RepositoryException {
        final Node child = node.getNode(subElement);
        child.remove();
    }

    @Override
    protected CreateOrUpdateResult<JSONNode> createOrUpdate(Node node, String subElement, JSONNode nodeData) throws RepositoryException {
        final Node newOrToUpdate;

        // is the child already existing? // todo: deal with same name siblings
        final boolean isUpdate = node.hasNode(subElement);
        if (isUpdate) {
            // in which case, we just want to update it
            newOrToUpdate = node.getNode(subElement);
        } else {
            // otherwise, we add the new node
            final String type = nodeData.getTypeName();
            if (type == null) {
                newOrToUpdate = node.addNode(subElement);
            } else {
                newOrToUpdate = node.addNode(subElement, type);
            }
        }

        NodeElementAccessor.initNodeFrom(newOrToUpdate, nodeData);

        return new CreateOrUpdateResult<JSONNode>(isUpdate, new JSONNode(newOrToUpdate, 1));
    }

    @Override
    protected String getSeeOtherURIAsString(Node node) {
        return URIUtils.getURIForChildren(node);
    }
}
