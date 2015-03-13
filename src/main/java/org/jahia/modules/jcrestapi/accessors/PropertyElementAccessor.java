/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2015 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either version 2
 *     of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 *
 *
 * ==========================================================================================
 * =                                   ABOUT JAHIA                                          =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia’s Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to “the Tunnel effect”, the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
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
        return mapper.readValue(rawJSONData, JSONProperty.class);
    }
}
