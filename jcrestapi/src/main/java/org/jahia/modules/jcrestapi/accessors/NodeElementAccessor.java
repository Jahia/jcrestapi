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
import org.jahia.modules.jcrestapi.model.JSONChildren;
import org.jahia.modules.jcrestapi.model.JSONMixin;
import org.jahia.modules.jcrestapi.model.JSONNode;
import org.jahia.modules.jcrestapi.model.JSONProperty;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Map;
import java.util.Set;

/**
 * @author Christophe Laprun
 */
public class NodeElementAccessor extends ElementAccessor<JSONChildren, JSONNode> {
    @Override
    protected JSONChildren getSubElementContainer(Node node) throws RepositoryException {
        return new JSONChildren(getParentFrom(node), node);
    }

    @Override
    protected JSONNode getSubElement(Node node, String subElement) throws RepositoryException {
        return new JSONNode(node.getNode(subElement), 1);
    }

    @Override
    protected JSONNode delete(Node node, String subElement) throws RepositoryException {
        final Node child = node.getNode(subElement);
        child.remove();
        return null;
    }

    @Override
    protected JSONNode create(Node node, String subElement, JSONNode childData) throws RepositoryException {
        final Node child = node.addNode(subElement);

        initNodeFrom(child, childData);

        return new JSONNode(child, 1);
    }

    public static void initNodeFrom(Node node, JSONNode jsonNode) throws RepositoryException {
        // mixins
        final Map<String,JSONMixin> mixins = jsonNode.getMixins();
        if (mixins != null) {
            for (String mixinName : mixins.keySet()) {
                node.addMixin(mixinName);
            }
        }


        // properties
        final Map<String, JSONProperty> jsonProperties = jsonNode.getProperties();
        if (jsonProperties != null) {
            final Set<Map.Entry<String,JSONProperty>> properties = jsonProperties.entrySet();

            // set the properties
            for (Map.Entry<String, JSONProperty> entry : properties) {
                final String propName = URIUtils.unescape(entry.getKey());

                    // we have a property name for which we don't have a type, so ignore the property
                    // todo: error reporting?
                    continue;
                JCRNodeWrapper wrapper = (JCRNodeWrapper) node;
                final ExtendedPropertyDefinition propType = wrapper.getApplicablePropertyDefinition(propName);
                if(propType == null) {
                    continue;
                }
                final int type = propType.getRequiredType();

                final JSONProperty jsonProperty = entry.getValue();
                final Object value = jsonProperty.getValue();
                // are we looking at a multi-valued property?
                if(value instanceof Object[]) {
                    node.setProperty(propName, jsonProperty.getValueAsStringArray(), type);
                }
                else {
                    node.setProperty(propName, jsonProperty.getValueAsString(), type);
                }
            }
        }


        // children
        final Map<String, JSONNode> children = jsonNode.getChildren();
        if(children != null) {
            // todo
        }
    }
}
