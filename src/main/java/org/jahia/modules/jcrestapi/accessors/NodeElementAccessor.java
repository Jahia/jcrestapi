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
package org.jahia.modules.jcrestapi.accessors;

import java.util.Map;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Christophe Laprun
 */
public class NodeElementAccessor extends ElementAccessor<JSONSubElementContainer<APIDecorator>, JSONNode<APIDecorator>, JSONNode> {
    private static final Logger logger = LoggerFactory.getLogger(NodeElementAccessor.class);

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

    /**
     * Applies the given representation to the given node: its mixins, its properties, then its children, recursively.
     *
     * <p>A representation is what a {@code GET} returns, so a client may send back a whole node it has just read. A
     * mixin or a property this API may not write is therefore skipped here, and the rest of the representation is
     * applied. The sub-element routes, where the request names one mixin or one property in the URL, refuse it
     * instead.</p>
     *
     * @param node     the node to apply the representation to
     * @param jsonNode the representation, may be {@code null}
     * @throws RepositoryException if the node cannot be written
     */
    public static void initNodeFrom(Node node, JSONNode<APIDecorator> jsonNode) throws RepositoryException {
        if (jsonNode == null) {
            return;
        }

        addMixinsTo(node, jsonNode.getMixins());
        setPropertiesOn(node, jsonNode.getProperties());
        addChildrenTo(node, jsonNode.getChildren());
    }

    private static void addMixinsTo(Node node, Map<String, JSONMixin<APIDecorator>> mixins) throws RepositoryException {
        if (mixins == null) {
            return;
        }

        for (String escapedName : mixins.keySet()) {
            final String mixinName = Names.unescape(escapedName);
            if (WriteRestrictions.isRestrictedMixin(mixinName)) {
                logger.warn("Ignoring mixin {} requested on {} (see jahia.api.jcr.restrictedMixins)", mixinName, node.getPath());
                continue;
            }
            node.addMixin(mixinName);
        }
    }

    private static void setPropertiesOn(Node node, Map<String, JSONProperty<APIDecorator>> jsonProperties) throws RepositoryException {
        if (jsonProperties == null) {
            return;
        }

        for (Map.Entry<String, JSONProperty<APIDecorator>> entry : jsonProperties.entrySet()) {
            final String propName = Names.unescape(entry.getKey());
            if (WriteRestrictions.isRestrictedProperty(propName, PropertyElementAccessor.getPropertyDefinitionOnNode(propName, node))) {
                logger.warn("Ignoring property {} requested on {} (see jahia.api.jcr.restrictedProperties)", propName, node.getPath());
                continue;
            }
            PropertyElementAccessor.setPropertyOnNode(entry.getKey(), entry.getValue(), node);
        }
    }

    private static void addChildrenTo(Node node, Map<String, JSONNode<APIDecorator>> children) throws RepositoryException {
        if (children == null) {
            return;
        }

        for (JSONNode<APIDecorator> jsonChild : children.values()) {
            final Node child = node.addNode(jsonChild.getName(), jsonChild.getTypeName());
            initNodeFrom(child, jsonChild);
        }
    }
}
