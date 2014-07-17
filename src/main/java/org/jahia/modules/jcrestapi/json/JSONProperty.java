/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.jcrestapi.json;

import org.jahia.modules.jcrestapi.URIUtils;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * @author Christophe Laprun
 */
@XmlRootElement
public class JSONProperty extends JSONItem<Property> {
    static final String JCR__PROPERTY_DEFINITION = "jcr__propertyDefinition";
    @XmlElement
    private boolean multiValued;

    @XmlElement
    private Object value;

    @XmlElement
    private boolean reference;

    private boolean path;

    public JSONProperty() {
    }

    public void initWith(Property property) throws RepositoryException {
        super.initWith(property);

        // check whether we need to add a target link
        final int type = property.getType();
        reference = type == PropertyType.PATH || type == PropertyType.REFERENCE || type == PropertyType.WEAKREFERENCE;
        path = PropertyType.PATH == type;

        // retrieve value
        this.multiValued = property.isMultiple();
        if (multiValued) {
            final Value[] values = property.getValues();
            value = new Object[values.length];

            for (int i = 0; i < values.length; i++) {
                final Value val = values[i];
                ((Object[]) value)[i] = convertValue(val);
            }
        } else {
            this.value = convertValue(property.getValue());
        }

        getDecorator().initFrom(this);
    }

    public JSONProperty(Property property) throws RepositoryException {
        initWith(property);
    }

    @Override
    public String getUnescapedTypeName(Property item) throws RepositoryException {
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
    public String getTypeChildPath(Property item) throws RepositoryException {
        // get declaring node type
        final NodeType declaringNodeType = item.getDefinition().getDeclaringNodeType();

        // get its name and escape it
        final String parentName = URIUtils.escape(declaringNodeType.getName());

        // get its property definitions
        final PropertyDefinition[] parentPropDefs = declaringNodeType.getDeclaredPropertyDefinitions();
        final int numberOfPropertyDefinitions = parentPropDefs.length;

        // if we only have one property definition, we're done
        if (numberOfPropertyDefinitions == 1) {
            return URIUtils.getChildURI(parentName, JCR__PROPERTY_DEFINITION, false);
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
            return URIUtils.getChildURI(parentName, URIUtils.escape(JCR__PROPERTY_DEFINITION, index), false);
        }
    }

    public Object getValue() {
        return value;
    }

    public String getValueAsString() {
        if (multiValued) {
            throw new IllegalStateException("Cannot call getValueAsString on a multi-valued property.");
        }
        return value.toString();
    }

    public String[] getValueAsStringArray() {
        if (!multiValued) {
            throw new IllegalStateException("Cannot call getValueAsStringArray on a simple-valued property.");
        }
        if (value instanceof Object[]) {
            // first check if we already have a String[]
            if (value.getClass().getComponentType().equals(String.class)) {
                return (String[]) value;
            } else {
                Object[] values = (Object[]) value;
                String[] result = new String[values.length];
                int i = 0;
                for (Object o : values) {
                    result[i++] = o.toString();
                }

                return result;
            }
        } else if (value instanceof List) {
            List values = (List) value;

            // first check if we don't already have a List of Strings
            if (!values.isEmpty() && values.get(0) instanceof String) {
                return (String[]) values.toArray(new String[values.size()]);
            } else {
                String[] result = new String[values.size()];
                int i = 0;
                for (Object o : values) {
                    result[i++] = o.toString();
                }

                return result;
            }

        } else {
            throw new IllegalArgumentException("Unknown value type: " + value.getClass().getSimpleName());
        }
    }

    public boolean isMultiValued() {
        return multiValued;
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
                break;
        }
        return theValue;
    }

    public boolean isReference() {
        return reference;
    }

    public boolean isPath() {
        return path;
    }
}
