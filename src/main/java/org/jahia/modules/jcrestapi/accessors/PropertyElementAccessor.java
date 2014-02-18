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
import org.jahia.modules.jcrestapi.model.JSONProperties;
import org.jahia.modules.jcrestapi.model.JSONProperty;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/**
 * @author Christophe Laprun
 */
public class PropertyElementAccessor extends ElementAccessor<JSONProperties, JSONProperty, JSONProperty> {
    static Property setPropertyOnNode(String escapedName, JSONProperty jsonProperty, Node node) throws RepositoryException {
        final String propName = URIUtils.unescape(escapedName);

        final Integer type = getTypeOfPropertyOnNode(propName, node);

        if (type == null) {
            // we have a property name for which we don't have a type, so ignore the property
            // todo: error reporting?
            return null;
        }

        final Object value = jsonProperty.getValue();
        // are we looking at a multi-valued property?
        if (value instanceof Object[]) {
            return node.setProperty(propName, jsonProperty.getValueAsStringArray(), type);
        } else {
            return node.setProperty(propName, jsonProperty.getValueAsString(), type);
        }
    }

    static Integer getTypeOfPropertyOnNode(String propName, Node node) throws RepositoryException {
        JCRNodeWrapper wrapper = (JCRNodeWrapper) node;
        final ExtendedPropertyDefinition propType = wrapper.getApplicablePropertyDefinition(propName);

        return propType == null ? null : propType.getRequiredType();
    }

    @Override
    protected JSONProperties getSubElementContainer(Node node) throws RepositoryException {
        return new JSONProperties(getParentFrom(node), node);
    }

    @Override
    protected JSONProperty getSubElement(Node node, String subElement) throws RepositoryException {
        return new JSONProperty(node.getProperty(subElement));
    }

    @Override
    protected void delete(Node node, String subElement) throws RepositoryException {
        node.setProperty(subElement, (Value) null);
    }

    @Override
    protected CreateOrUpdateResult<JSONProperty> createOrUpdate(Node node, String subElement, JSONProperty childData) throws RepositoryException {
        final boolean isUpdate = node.hasProperty(subElement);
        final Property property = setPropertyOnNode(subElement, childData, node);
        return new CreateOrUpdateResult<JSONProperty>(isUpdate, new JSONProperty(property));
    }
}
