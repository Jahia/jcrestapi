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
package org.jahia.modules.jcrestapi.accessors;

import org.jahia.modules.jcrestapi.URIUtils;
import org.jahia.modules.jcrestapi.model.JSONMixin;
import org.jahia.modules.jcrestapi.model.JSONNode;
import org.jahia.modules.jcrestapi.model.JSONProperty;
import org.jahia.modules.jcrestapi.model.JSONSubElementContainer;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Map;
import java.util.Set;

/**
 * @author Christophe Laprun
 */
public class NodeElementAccessor extends ElementAccessor<JSONSubElementContainer, JSONNode, JSONNode> {
    @Override
    protected Object getElement(Node node, String subElement) throws RepositoryException {
        return new JSONNode(node, 1);
    }

    @Override
    protected JSONSubElementContainer getSubElementContainer(Node node) throws RepositoryException {
        throw new UnsupportedOperationException("Cannot call getSubElementContainer on NodeElementAccessor");
    }

    @Override
    protected JSONNode getSubElement(Node node, String subElement) throws RepositoryException {
        throw new UnsupportedOperationException("Cannot call getSubElement on NodeElementAccessor");
    }

    @Override
    protected void delete(Node node, String subElement) throws RepositoryException {
        node.remove();
    }

    @Override
    protected CreateOrUpdateResult<JSONNode> createOrUpdate(Node node, String subElement, JSONNode nodeData) throws RepositoryException {
        initNodeFrom(node, nodeData);

        // update only scenario at the moment
        return new CreateOrUpdateResult<JSONNode>(true, new JSONNode(node, 1));
    }

    @Override
    protected String getSeeOtherURIAsString(Node node) {
        throw new UnsupportedOperationException("Cannot call getSeeOtherURIAsString on NodeElementAccessor");
    }

    public static void initNodeFrom(Node node, JSONNode jsonNode) throws RepositoryException {
        // mixins
        final Map<String, JSONMixin> mixins = jsonNode.getMixins();
        if (mixins != null) {
            for (String mixinName : mixins.keySet()) {
                mixinName = URIUtils.unescape(mixinName);
                node.addMixin(mixinName);
            }
        }


        // properties
        final Map<String, JSONProperty> jsonProperties = jsonNode.getProperties();
        if (jsonProperties != null) {
            final Set<Map.Entry<String, JSONProperty>> properties = jsonProperties.entrySet();

            // set the properties
            for (Map.Entry<String, JSONProperty> entry : properties) {
                PropertyElementAccessor.setPropertyOnNode(entry.getKey(), entry.getValue(), node);
            }
        }


        // children
        final Map<String, JSONNode> children = jsonNode.getChildren();
        if (children != null) {
            for (JSONNode jsonChild : children.values()) {
                final Node child = node.addNode(jsonChild.getName(), jsonChild.getTypeName());
                initNodeFrom(child, jsonChild);
            }
        }
    }
}
