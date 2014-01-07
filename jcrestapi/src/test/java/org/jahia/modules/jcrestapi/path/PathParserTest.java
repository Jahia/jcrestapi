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

import org.junit.Test;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Christophe Laprun
 */
public class PathParserTest {

    @Test
    public void testGetRoot() {
        AccessorPair accessors = getAccessorsForPath("");

        assertTrue(accessors.nodeAccessor instanceof PathNodeAccessor);
        assertEquals("/", accessors.nodeAccessor.getNodePath());

        assertEquals(accessors.itemAccessor, ItemAccessor.IDENTITY_ACCESSOR);

        accessors = getAccessorsForPath("/");

        assertTrue(accessors.nodeAccessor instanceof PathNodeAccessor);
        assertEquals("/", accessors.nodeAccessor.getNodePath());

        assertEquals(accessors.itemAccessor, ItemAccessor.IDENTITY_ACCESSOR);
    }

    private AccessorPair getAccessorsForPath(String path) {
        final String[] split = path.split("/");
        List<PathSegment> segments = new ArrayList<PathSegment>(split.length + 1);
        segments.add(createPathSegment("api"));
        for (String s : split) {
            if (!s.isEmpty()) {
                segments.add(createPathSegment(s));
            }
        }

        return PathParser.getAccessorsForPath(null, segments);
    }

    private PathSegment createPathSegment(final String path) {
        return new PathSegment() {
            @Override
            public String getPath() {
                return path;
            }

            @Override
            public MultivaluedMap<String, String> getMatrixParameters() {
                return null;
            }
        };
    }

    @Test
    public void testGetRootProperties() {
        AccessorPair accessors = getAccessorsForPath("properties");

        assertTrue(accessors.nodeAccessor instanceof PathNodeAccessor);
        assertEquals("/", accessors.nodeAccessor.getNodePath());

        assertTrue(accessors.itemAccessor instanceof PropertiesAccessor);

        accessors = getAccessorsForPath("/properties");

        assertTrue(accessors.nodeAccessor instanceof PathNodeAccessor);
        assertEquals("/", accessors.nodeAccessor.getNodePath());

        assertTrue(accessors.itemAccessor instanceof PropertiesAccessor);
    }

    @Test
    public void testGetRootProperty() {
        AccessorPair accessors = getAccessorsForPath("/properties/property");

        assertTrue(accessors.nodeAccessor instanceof PathNodeAccessor);
        assertEquals("/", accessors.nodeAccessor.getNodePath());

        assertTrue(accessors.itemAccessor instanceof PropertyAccessor);
        assertEquals("property", ((PropertyAccessor) accessors.itemAccessor).getPropertyName());

        accessors = getAccessorsForPath("properties/property");

        assertTrue(accessors.nodeAccessor instanceof PathNodeAccessor);
        assertEquals("/", accessors.nodeAccessor.getNodePath());

        assertTrue(accessors.itemAccessor instanceof PropertyAccessor);
        assertEquals("property", ((PropertyAccessor) accessors.itemAccessor).getPropertyName());
    }

    @Test
    public void testEnsureAbsolutePath() {
        checkNode("foo/bar/baz");
    }

    @Test
    public void testGetNodeWithTrailingSlash() {
        checkNode("/foo/barbar/buzz/");
    }

    @Test
    public void testGetNode() {
        checkNode("/foo/bar/baz");
    }

    @Test
    public void testEnsureUnescaping() {
        String path = "/jcr__system/jcr__nodeTypes/nt__base/jcr__propertyDefinition--2/properties/jcr__protected";

        final AccessorPair accessors = getAccessorsForPath(path);

        assertTrue(accessors.nodeAccessor instanceof PathNodeAccessor);
        assertEquals("/jcr:system/jcr:nodeTypes/nt:base/jcr:propertyDefinition[2]", accessors.nodeAccessor.getNodePath());

        assertTrue(accessors.itemAccessor instanceof PropertyAccessor);
        assertEquals("jcr:protected", ((PropertyAccessor) accessors.itemAccessor).getPropertyName());
    }

    private void checkNode(String path) {
        final AccessorPair accessors = getAccessorsForPath(path);

        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        assertTrue(accessors.nodeAccessor instanceof PathNodeAccessor);
        assertEquals(path, accessors.nodeAccessor.getNodePath());

        assertEquals(accessors.itemAccessor, ItemAccessor.IDENTITY_ACCESSOR);
    }

    @Test
    public void testGetProperty() {
        final AccessorPair accessors = getAccessorsForPath("/foo/bar/baz/properties/property");

        assertTrue(accessors.nodeAccessor instanceof PathNodeAccessor);
        assertEquals("/foo/bar/baz", accessors.nodeAccessor.getNodePath());

        assertTrue(accessors.itemAccessor instanceof PropertyAccessor);
        assertEquals("property", ((PropertyAccessor) accessors.itemAccessor).getPropertyName());
    }

    @Test
    public void testGetProperties() {
        AccessorPair accessors = getAccessorsForPath("/foo/bar/baz/properties/");

        assertTrue(accessors.nodeAccessor instanceof PathNodeAccessor);
        assertEquals("/foo/bar/baz", accessors.nodeAccessor.getNodePath());

        assertTrue(accessors.itemAccessor instanceof PropertiesAccessor);

        accessors = getAccessorsForPath("/foo/bar/baz/properties");

        assertTrue(accessors.nodeAccessor instanceof PathNodeAccessor);
        assertEquals("/foo/bar/baz", accessors.nodeAccessor.getNodePath());

        assertTrue(accessors.itemAccessor instanceof PropertiesAccessor);
    }

    @Test
    public void testGetChildren() {
        AccessorPair accessors = getAccessorsForPath("/foo/bar/baz/children/");

        assertTrue(accessors.nodeAccessor instanceof PathNodeAccessor);
        assertEquals("/foo/bar/baz", accessors.nodeAccessor.getNodePath());

        assertTrue(accessors.itemAccessor instanceof ChildrenAccessor);
    }

    @Test
    public void testGetMixins() {
        AccessorPair accessors = getAccessorsForPath("/foo/mixins/");

        assertTrue(accessors.nodeAccessor instanceof PathNodeAccessor);
        assertEquals("/foo", accessors.nodeAccessor.getNodePath());

        assertTrue(accessors.itemAccessor instanceof MixinsAccessor);
    }

    @Test
    public void testGetVersions() {
        AccessorPair accessors = getAccessorsForPath("/foo/bar/versions/");

        assertTrue(accessors.nodeAccessor instanceof PathNodeAccessor);
        assertEquals("/foo/bar", accessors.nodeAccessor.getNodePath());

        assertTrue(accessors.itemAccessor instanceof VersionsAccessor);
    }

    /*@Test
    public void testPerf() {
        final long begin = System.currentTimeMillis();

        for (int i = 0; i < 10000000; i++) {
            getAccessorsForPath("/foo/bar/baz/properties/property");
        }

        System.out.println("Time: " + (System.currentTimeMillis() - begin) );
    }*/
}
