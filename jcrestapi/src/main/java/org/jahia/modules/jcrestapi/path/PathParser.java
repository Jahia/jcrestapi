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
package org.jahia.modules.jcrestapi.path;

/**
 * @author Christophe Laprun
 */
public class PathParser {
    static public AccessorPair getAccessorsForPath(String path) {

        // while we can find a / in path, starting from offset
        int next, offset = 0;
        while ((next = path.indexOf('/', offset)) != -1) {
            // extract segment
            final String segment = path.substring(offset, next);

            // only consider segment if it's not empty
            if (!segment.isEmpty()) {
                // check if segment is a sub-element marker
                final AccessorPair pair = analyzeSegment(segment);
                if (pair != null) {
                    // we've found a sub-element marker, so we're done
                    pair.initWith(
                            normalizeNodePath(path.substring(0, offset)),
                            path.substring(offset + segment.length() + 1, path.length())
                    );
                    return pair;
                }
            }

            // move offset
            offset = next + 1;
        }

        // check if we're asking root sub-elements
        final AccessorPair accessorPair = analyzeSegment(path);
        if (accessorPair != null) {
            // init node accessor to access root
            accessorPair.initWith("/", null);
            return accessorPair;
        } else {
            // we haven't found a sub-element query so return the node
            String node = normalizeNodePath(path);
            return new AccessorPair(new PathNodeAccessor(node), ItemAccessor.IDENTITY_ACCESSOR);
        }
    }

    private static String normalizeNodePath(String path) {
        String node;
        if (path.isEmpty() || "/".equals(path)) {
            node = "/";
        } else {
            node = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
            node = path.startsWith("/") ? node : "/" + node;
        }
        return node;
    }

    private static AccessorPair analyzeSegment(String segment) {
        if ("properties".equals(segment)) {
            return new AccessorPair(new PathNodeAccessor(), new PropertyAccessor());
        }

        if ("children".equals(segment)) {
            return new AccessorPair(new PathNodeAccessor(), new ChildrenAccessor());
        }

        if ("mixins".equals(segment)) {
            return new AccessorPair(new PathNodeAccessor(), new MixinsAccessor());
        }

        return null;
    }
}
