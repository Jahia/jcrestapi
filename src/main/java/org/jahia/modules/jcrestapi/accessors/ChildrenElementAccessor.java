/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
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

import org.jahia.api.Constants;
import org.jahia.modules.jcrestapi.URIUtils;
import org.jahia.modules.jcrestapi.Utils;
import org.jahia.modules.jcrestapi.links.APIDecorator;
import org.jahia.modules.json.JSONChildren;
import org.jahia.modules.json.JSONNode;
import org.jahia.modules.json.JSONProperty;
import org.jahia.services.content.JCRContentUtils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.UriInfo;

/**
 * @author Christophe Laprun
 */
public class ChildrenElementAccessor extends ElementAccessor<JSONChildren<APIDecorator>, JSONNode<APIDecorator>, JSONNode> {
    @Override
    protected JSONChildren<APIDecorator> getSubElementContainer(Node node, UriInfo context) throws RepositoryException {
        int depth = Utils.getDepthFrom(context, 1);

        return getFactory().createChildren(getParentFrom(node), node, Utils.getFilter(context), depth);
    }

    @Override
    protected JSONNode<APIDecorator> getSubElement(Node node, String subElement, UriInfo context) throws RepositoryException {
        return getFactory().createNode(node.getNode(subElement), Utils.getFilter(context), 1);
    }

    @Override
    protected void delete(Node node, String subElement) throws RepositoryException {
        final Node child = node.getNode(subElement);
        child.remove();
    }

    @Override
    protected CreateOrUpdateResult<JSONNode<APIDecorator>> createOrUpdate(Node node, String subElement, JSONNode nodeData) throws RepositoryException {
        final Node newOrToUpdate;

        // is the child already existing? // todo: deal with same name siblings
        final boolean hasSubElement = subElement != null && !subElement.isEmpty();
        final boolean isUpdate = hasSubElement && node.hasNode(subElement);
        if (isUpdate) {
            // in which case, we just want to update it
            newOrToUpdate = node.getNode(subElement);
        } else {
            // otherwise, we add the new node
            final String type = nodeData.getTypeName();

            // if we didn't get a subElement name, generate one
            if (!hasSubElement) {
                final JSONProperty title = nodeData.getProperty(Constants.JCR_TITLE);
                if (title != null) {
                    // if we have a jcr:title in the node data, use it as basis
                    subElement = JCRContentUtils.generateNodeName(title.getValueAsString());
                } else {
                    // otherwise generate node name from node type
                    subElement = JCRContentUtils.generateNodeName(type);
                }

                // then make sure we get an available name from the basis we used
                subElement = JCRContentUtils.findAvailableNodeName(node, subElement);
            }

            if (type == null) {
                newOrToUpdate = node.addNode(subElement);
            } else {
                newOrToUpdate = node.addNode(subElement, type);
            }
        }

        NodeElementAccessor.initNodeFrom(newOrToUpdate, nodeData);

        return new CreateOrUpdateResult<JSONNode<APIDecorator>>(isUpdate, getFactory().createNode(newOrToUpdate, 1));
    }

    @Override
    protected String getSeeOtherURIAsString(Node node) {
        return URIUtils.getURIForChildren(node);
    }
}
