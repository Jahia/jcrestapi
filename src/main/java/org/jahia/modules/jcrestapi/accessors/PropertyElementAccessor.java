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

import java.util.List;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.ObjectReader;
import org.jahia.modules.jcrestapi.URIUtils;
import org.jahia.modules.jcrestapi.links.APIDecorator;
import org.jahia.modules.json.JSONItem;
import org.jahia.modules.json.JSONProperties;
import org.jahia.modules.json.JSONProperty;
import org.jahia.modules.json.Names;
import org.jahia.services.content.JCRNodeWrapper;

/**
 * @author Christophe Laprun
 */
public class PropertyElementAccessor extends ElementAccessor<JSONProperties<APIDecorator>, JSONProperty<APIDecorator>, JSONProperty> {
    private static final ObjectReader reader = mapper.reader(JSONProperty.class);

    static Property setPropertyOnNode(String escapedName, JSONProperty jsonProperty, Node node) throws RepositoryException {
        final String propName = Names.unescape(escapedName);

        final Integer type = getTypeOfPropertyOnNode(propName, node);

        if (type == null) {
            // we have a property name for which we don't have a type, so ignore the property
            // todo: error reporting?
            return null;
        }

        final Object value = jsonProperty.getValue();
        // are we looking at a multi-valued property?
        if (value instanceof Object[] || value instanceof List) {
            return node.setProperty(propName, jsonProperty.getValueAsStringArray(), type);
        } else {
            return node.setProperty(propName, jsonProperty.getValueAsString(), type);
        }
    }

    static Integer getTypeOfPropertyOnNode(String propName, Node node) throws RepositoryException {
        PropertyDefinition propType;

        if (node instanceof JCRNodeWrapper) {
            JCRNodeWrapper wrapper = (JCRNodeWrapper) node;
            propType = wrapper.getApplicablePropertyDefinition(propName);
        } else {
            final NodeType type = node.getPrimaryNodeType();
            final PropertyDefinition[] propertyDefinitions = type.getPropertyDefinitions();
            propType = getPropertyDefinitionFrom(propName, propertyDefinitions);
        }

        return propType == null ? null : propType.getRequiredType();
    }

    public static PropertyDefinition getPropertyDefinitionFrom(String propName, PropertyDefinition[] propertyDefinitions) {
        PropertyDefinition propType = null;
        for (PropertyDefinition definition : propertyDefinitions) {
            if (definition.getName().equals(propName)) {
                propType = definition;
                break;
            }
        }
        return propType;
    }

    @Override
    protected JSONProperties<APIDecorator> getSubElementContainer(Node node, UriInfo context) throws RepositoryException {
        return getFactory().createProperties(getParentFrom(node), node);
    }

    @Override
    protected JSONProperty<APIDecorator> getSubElement(Node node, String subElement, UriInfo context) throws RepositoryException {
        return getFactory().createProperty(node.getProperty(subElement));
    }

    @Override
    protected void delete(Node node, String subElement) throws RepositoryException {
        node.setProperty(subElement, (Value) null);
    }

    @Override
    protected CreateOrUpdateResult<JSONProperty<APIDecorator>> createOrUpdate(Node node, String subElement, JSONProperty childData) throws RepositoryException {
        if(subElement == null || subElement.isEmpty()) {
            throw new UnsupportedOperationException("Cannot create automatically named properties");
        }
        final boolean isUpdate = node.hasProperty(subElement);
        final Property property = setPropertyOnNode(subElement, childData, node);
        return new CreateOrUpdateResult<JSONProperty<APIDecorator>>(isUpdate, getFactory().createProperty(property));
    }

    @Override
    protected String getSeeOtherURIAsString(Node node) {
        return URIUtils.getURIForProperties(node);
    }

    @Override
    public JSONItem convertFrom(String rawJSONData) throws Exception {
        return reader.readValue(rawJSONData);
    }
}
