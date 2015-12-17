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
 *     This program is free software; you can redistribute it and/or
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
package org.jahia.modules.jcrestapi;

import org.jahia.modules.json.Filter;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utilities class.
 *
 * @author Christophe Laprun
 */
public class Utils {

    /**
     * Retrieves whether or not the specified String is not null and not empty.
     *
     * @param string the String to be tested
     * @return <code>true</code> if the specified String is not null and not empty, <code>false</code> otherwise
     */
    public static boolean exists(String string) {
        return string != null && !string.isEmpty();
    }

    public static Set<String> split(String string) {
        if(!exists(string)) {
            return Collections.emptySet();
        }

        final Set<String> result = new HashSet<String>();
        int comma = string.indexOf(',');
        int previousComma = 0;
        final int length = string.length();
        while(comma >= 0 && comma < length) {
            addSubstringToSetIfNotEmpty(string, result, previousComma, comma);
            previousComma = comma + 1;
            comma = string.indexOf(',', previousComma);
        }

        if(previousComma < length) {
            addSubstringToSetIfNotEmpty(string, result, previousComma, length);
        }

        return result;
    }

    private static void addSubstringToSetIfNotEmpty(String string, Set<String> result, int begin, int end) {
        final String trimmed = string.substring(begin, end).trim();
        if (!trimmed.isEmpty()) {
            result.add(trimmed);
        }
    }

    public static int getDepthFrom(UriInfo context, int defaultDepth) {
        return getFlagValueFrom(context, API.INCLUDE_FULL_CHILDREN) ? defaultDepth + 1 : defaultDepth;
    }

    public static class ChildrenNodeTypeFilter extends Filter.DefaultFilter {

        Set<String> acceptedChildrenNodeTypes;

        public ChildrenNodeTypeFilter(Set<String> acceptedChildrenNodeTypes) {
            this.acceptedChildrenNodeTypes = acceptedChildrenNodeTypes;
        }

        @Override
        public boolean acceptChild(Node child) {
            for (String acceptedChildrenNodeType : acceptedChildrenNodeTypes) {
                try {
                    if (child.isNodeType(acceptedChildrenNodeType)) {
                        return true;
                    }
                } catch (RepositoryException e) {
                    return false;
                }
            }
            return false;
        }
    }

    public static Filter getFilter(UriInfo context) {
        final MultivaluedMap<String, String> queryParameters = context.getQueryParameters();
        if (queryParameters != null) {
            final List<String> childrenNodeTypeFilterValues = queryParameters.get(API.CHILDREN_NODETYPE_FILTER);
            if (childrenNodeTypeFilterValues != null) {
                if (!childrenNodeTypeFilterValues.isEmpty()) {
                    Set<String> childrenNodeTypes = new HashSet<String>();
                    for (String childrenNodeTypeFilterValue : childrenNodeTypeFilterValues) {
                        childrenNodeTypes.addAll(split(childrenNodeTypeFilterValue));
                    }
                    if (!childrenNodeTypes.isEmpty()) {
                        final ChildrenNodeTypeFilter nodeTypeFilter = new ChildrenNodeTypeFilter(childrenNodeTypes);

                        // wrap filter so that we can always exclude first excluded node types
                        return new Filter.DefaultFilter() {
                            @Override
                            public boolean acceptChild(Node child) {
                                return API.NODE_FILTER.acceptChild(child) && nodeTypeFilter.acceptChild(child);
                            }
                        };
                    }
                }
            }
        }
        return API.NODE_FILTER;
    }

    public static boolean getFlagValueFrom(UriInfo context, String flagName) {
        final MultivaluedMap<String, String> queryParameters = context.getQueryParameters();
        if (queryParameters != null) {
            final List<String> flagValues = queryParameters.get(flagName);
            if (flagValues != null) {
                if (flagValues.isEmpty() || !"false".equals(flagValues.get(0))) {
                    return true;
                }
            }
        }
        return false;
    }
}
