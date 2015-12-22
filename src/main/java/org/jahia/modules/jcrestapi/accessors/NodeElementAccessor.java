/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
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

import java.util.Map;
import java.util.Set;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.UriInfo;

import org.jahia.modules.jcrestapi.Utils;
import org.jahia.modules.jcrestapi.links.APIDecorator;
import org.jahia.modules.json.JSONMixin;
import org.jahia.modules.json.JSONNode;
import org.jahia.modules.json.JSONProperty;
import org.jahia.modules.json.JSONSubElementContainer;
import org.jahia.modules.json.Names;

/**
 * @author Christophe Laprun
 */
public class NodeElementAccessor extends ElementAccessor<JSONSubElementContainer<APIDecorator>, JSONNode<APIDecorator>, JSONNode> {
    @Override
    protected Object getElement(Node node, String subElement, UriInfo context) throws RepositoryException {
        return getFactory().createNode(node, Utils.getFilter(context), Utils.getDepthFrom(context, 1));
    }

    @Override
    protected JSONSubElementContainer<APIDecorator> getSubElementContainer(Node node, UriInfo context) throws RepositoryException {
        throw new UnsupportedOperationException("Cannot call getSubElementContainer on NodeElementAccessor");
    }

    @Override
    protected JSONNode<APIDecorator> getSubElement(Node node, String subElement, UriInfo context) throws RepositoryException {
        throw new UnsupportedOperationException("Cannot call getSubElement on NodeElementAccessor");
    }

    @Override
    protected void delete(Node node, String subElement) throws RepositoryException {
        node.remove();
    }

    @Override
    protected CreateOrUpdateResult<JSONNode<APIDecorator>> createOrUpdate(Node node, String subElement, JSONNode nodeData) throws RepositoryException {
        initNodeFrom(node, nodeData);

        // update only scenario at the moment
        return new CreateOrUpdateResult<JSONNode<APIDecorator>>(true, getFactory().createNode(node, 1));
    }

    @Override
    protected String getSeeOtherURIAsString(Node node) {
        throw new UnsupportedOperationException("Cannot call getSeeOtherURIAsString on NodeElementAccessor");
    }

    public static void initNodeFrom(Node node, JSONNode<APIDecorator> jsonNode) throws RepositoryException {
        if (jsonNode != null) {
            // mixins
            final Map<String, JSONMixin<APIDecorator>> mixins = jsonNode.getMixins();
            if (mixins != null) {
                for (String mixinName : mixins.keySet()) {
                    mixinName = Names.unescape(mixinName);
                    node.addMixin(mixinName);
                }
            }

            // properties
            final Map<String, JSONProperty<APIDecorator>> jsonProperties = jsonNode.getProperties();
            if (jsonProperties != null) {
                final Set<Map.Entry<String, JSONProperty<APIDecorator>>> properties = jsonProperties.entrySet();

                // set the properties
                for (Map.Entry<String, JSONProperty<APIDecorator>> entry : properties) {
                    PropertyElementAccessor.setPropertyOnNode(entry.getKey(), entry.getValue(), node);
                }
            }

            // children
            final Map<String, JSONNode<APIDecorator>> children = jsonNode.getChildren();
            if (children != null) {
                for (JSONNode<APIDecorator> jsonChild : children.values()) {
                    final Node child = node.addNode(jsonChild.getName(), jsonChild.getTypeName());
                    initNodeFrom(child, jsonChild);
                }
            }
        }
    }
}
