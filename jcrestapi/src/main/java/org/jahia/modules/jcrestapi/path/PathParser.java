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

import org.jahia.modules.jcrestapi.API;

import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Christophe Laprun
 */
public class PathParser {
    public static AccessorPair getAccessorsForPath(UriBuilder baseUriBuilder, List<PathSegment> segments) {
        int index = 0;
        for (PathSegment segment : segments) {
            // first path segment corresponds to the resource mapping so we ignore it
            if (index != 0) {

                // check if segment is a sub-element marker
                final AccessorPair pair = analyzeSegment(SegmentContext.forPathSegment(segment, segments, index, baseUriBuilder));
                if (pair != null) {
                    // we've found a sub-element marker, so we're done
                    return pair;
                }
            }
            baseUriBuilder.segment(segment.getPath());
            index++;
        }

        return new AccessorPair(new PathNodeAccessor(computePathUpTo(segments, segments.size()), baseUriBuilder.build()),
                ItemAccessor.IDENTITY_ACCESSOR);
    }

    private static AccessorPair analyzeSegment(SegmentContext context) {
        final String segment = context.getSegment();
        final AccessorPairGenerator accessors = generators.get(segment);
        if (accessors != null) {
            return accessors.getAccessorPair(context);
        } else {
            return null;
        }
    }

    private static abstract class SegmentContext {
        abstract String getSegment();

        abstract String getNodePath();

        abstract String getSubElement();

        abstract URI getNodeURI();

        static SegmentContext forPathSegment(final PathSegment segment, final List<PathSegment> segments,
                                             final int index, final UriBuilder baseURIBuilder) {
            return new SegmentContext() {
                @Override
                String getSegment() {
                    return segment.getPath();
                }

                @Override
                String getNodePath() {
                    return computePathUpTo(segments, index);
                }

                @Override
                String getSubElement() {
                    final int next = index + 1;
                    if (next < segments.size()) {
                        return segments.get(next).getPath();
                    } else {
                        return "";
                    }
                }

                @Override
                URI getNodeURI() {
                    return baseURIBuilder.build();
                }
            };
        }
    }

    private static String computePathUpTo(List<PathSegment> segments, int index) {
        StringBuilder path = new StringBuilder(30 * index);
        // first path segment corresponds to the resource mapping so we ignore it
        for (int i = 1; i < index; i++) {
            path.append("/").append(segments.get(i).getPath());
        }
        return path.toString();
    }

    private static interface AccessorPairGenerator {
        AccessorPair getAccessorPair(SegmentContext context);

        AccessorPairGenerator properties = new AccessorPairGenerator() {
            @Override
            public AccessorPair getAccessorPair(SegmentContext context) {
                final String subElement = context.getSubElement();
                if (subElement.isEmpty()) {
                    return new AccessorPair(new PathNodeAccessor(context.getNodePath(), context.getNodeURI()),
                            new PropertiesAccessor());
                } else {
                    return new AccessorPair(new PathNodeAccessor(context.getNodePath(), context.getNodeURI()), new PropertyAccessor(subElement));
                }
            }
        };

        AccessorPairGenerator children = new AccessorPairGenerator() {
            @Override
            public AccessorPair getAccessorPair(SegmentContext context) {
                return new AccessorPair(new PathNodeAccessor(context.getNodePath(), context.getNodeURI()), new ChildrenAccessor());
            }
        };

        AccessorPairGenerator mixins = new AccessorPairGenerator() {
            @Override
            public AccessorPair getAccessorPair(SegmentContext context) {
                return new AccessorPair(new PathNodeAccessor(context.getNodePath(), context.getNodeURI()), new MixinsAccessor());
            }
        };

        AccessorPairGenerator versions = new AccessorPairGenerator() {
            @Override
            public AccessorPair getAccessorPair(SegmentContext context) {
                return new AccessorPair(new PathNodeAccessor(context.getNodePath(), context.getNodeURI()), new VersionsAccessor());
            }
        };
    }

    private static final Map<String, AccessorPairGenerator> generators = new HashMap<String, AccessorPairGenerator>(7);

    static {
        generators.put(API.PROPERTIES, AccessorPairGenerator.properties);
        generators.put(API.CHILDREN, AccessorPairGenerator.children);
        generators.put(API.MIXINS, AccessorPairGenerator.mixins);
        generators.put(API.VERSIONS, AccessorPairGenerator.versions);
    }
}
