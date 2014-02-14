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

import org.jahia.modules.jcrestapi.API;
import org.jahia.modules.jcrestapi.URIUtils;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Christophe Laprun
 */
@XmlRootElement
public class JSONProperty extends JSONItem<Property> {
    @XmlElement
    private boolean multiple;

    @XmlElement
    private Object value;

    @XmlElement
    private boolean reference;

    public JSONProperty() {
    }

    public void initWith(Property property) throws RepositoryException {
        super.initWith(property);

        // check whether we need to add a target link
        final int type = property.getType();
        reference = type == PropertyType.PATH || type == PropertyType.REFERENCE || type == PropertyType.WEAKREFERENCE;

        // retrieve value
        this.multiple = property.isMultiple();
        if (multiple) {
            final Value[] values = property.getValues();
            value = new Object[values.length];
            String[] links = null;
            if (reference) {
                links = new String[values.length];
            }

            for (int i = 0; i < values.length; i++) {
                final Value val = values[i];
                ((Object[]) value)[i] = convertValue(val);
                if (reference) {
                    links[i] = URIUtils.getIdURI(val.getString());
                }
            }

            if (reference) {
                addLink(new JSONLink(API.TARGET, links));
            }
        } else {
            this.value = convertValue(property.getValue());
            if (reference) {
                addLink(new JSONLink(API.TARGET, URIUtils.getIdURI(property.getString())));
            }
        }
    }

    public JSONProperty(Property property) throws RepositoryException {
        initWith(property);
    }

    @Override
    protected String getUnescapedTypeName(Property item) throws RepositoryException {
        return getHumanReadablePropertyType(item.getType());
    }

    static String getHumanReadablePropertyType(int type) throws RepositoryException {
        return PropertyType.nameFromValue(type);
    }

    /**
     * Property types are a little bit more tricky: we need to get the declaring node type and figure out in its array of property definitions which one matches our property to be
     * able to build the proper type link.
     *
     * @param item
     * @return
     * @throws RepositoryException
     */
    @Override
    protected String getTypeChildPath(Property item) throws RepositoryException {
        // get declaring node type
        final NodeType declaringNodeType = item.getDefinition().getDeclaringNodeType();

        // get its name and escape it
        final String parentName = URIUtils.escape(declaringNodeType.getName());

        // get its property definitions
        final PropertyDefinition[] parentPropDefs = declaringNodeType.getDeclaredPropertyDefinitions();
        final int numberOfPropertyDefinitions = parentPropDefs.length;

        // if we only have one property definition, we're done
        if (numberOfPropertyDefinitions == 1) {
            return URIUtils.getChildURI(parentName, "jcr__propertyDefinition", false);
        } else {
            // we need to figure out which property definition matches ours in the array
            int index = 1; // JCR indexes start at 1
            for (int i = 0; i < numberOfPropertyDefinitions; i++) {
                PropertyDefinition propDef = parentPropDefs[i];
                if (propDef.getName().equals(item.getName())) {
                    index = i + 1; // adjust for start at 1 in JCR
                    break;
                }
            }
            // create the indexed escaped link, if index = 1, no need for an index
            return URIUtils.getChildURI(parentName, URIUtils.escape("jcr__propertyDefinition", index), false);
        }
    }

    public Object getValue() {
        return value;
    }

    public String getValueAsString() {
        if (multiple) {
            throw new IllegalStateException("Cannot call getValueAsString on property with multiple values.");
        }
        return value.toString();
    }

    public String[] getValueAsStringArray() {
        if (!multiple) {
            throw new IllegalStateException("Cannot call getValueAsStringArray on property with non-multiple values.");
        }
        Object[] values = (Object[]) value;
        String[] result = new String[values.length];
        int i = 0;
        for (Object o : values) {
            result[i++] = o.toString();
        }

        return result;
    }

    public boolean isMultiple() {
        return multiple;
    }

    public static Object convertValue(Value val) throws RepositoryException {
        Object theValue;
        if (val == null) {
            return null;
        }
        switch (val.getType()) {
            case PropertyType.BINARY:
                theValue = val.getString();
                break;
            case PropertyType.BOOLEAN:
                theValue = val.getBoolean();
                break;
            case PropertyType.DATE:
                theValue = val.getDate();
                break;
            case PropertyType.DOUBLE:
                theValue = val.getDouble();
                break;
            case PropertyType.LONG:
                theValue = val.getLong();
                break;
            case PropertyType.NAME:
                theValue = val.getString();
                break;
            case PropertyType.PATH:
                theValue = val.getString();
                break;
            case PropertyType.REFERENCE:
                theValue = val.getString();
                break;
            case PropertyType.STRING:
                theValue = val.getString();
                break;
            case PropertyType.UNDEFINED:
                theValue = val.getString();
                break;
            default:
                theValue = val.getString();
        }
        return theValue;
    }
}
