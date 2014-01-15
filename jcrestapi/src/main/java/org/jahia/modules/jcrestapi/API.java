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
package org.jahia.modules.jcrestapi;

import org.jahia.modules.jcrestapi.model.*;
import org.osgi.service.component.annotations.Component;

import javax.jcr.*;
import javax.jcr.nodetype.NodeType;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Christophe Laprun
 */
@Component
@Path(API.API_PATH)
@Produces({MediaType.APPLICATION_JSON})
public class API {
    public static final String VERSION;

    static final String API_PATH = "/api";

    public static final String PROPERTIES = "properties";
    public static final String MIXINS = "mixins";
    public static final String CHILDREN = "children";
    public static final String VERSIONS = "versions";
    public static final String TYPE = "type";

    private final static Map<String, ElementAccessor> accessors = new HashMap<String, ElementAccessor>(7);

    static {
        Properties props = new Properties();
        try {
            props.load(API.class.getClassLoader().getResourceAsStream("jcrestapi.properties"));
        } catch (Exception e) {
            throw new RuntimeException("Could not load jcrestapi.properties.");
        }

        VERSION = props.getProperty("jcrestapi.version");

        accessors.put(PROPERTIES, new ElementAccessor<JSONProperties, JSONProperty>() {
            @Override
            JSONProperties getSubElementContainer(Node node) throws RepositoryException {
                return new JSONProperties(getParentFrom(node), node);
            }

            @Override
            JSONProperty getSubElement(Node node, String subElement) throws RepositoryException {
                return new JSONProperty(node.getProperty(subElement));
            }

            @Override
            JSONProperty delete(Node node, String subElement) throws RepositoryException {
                node.setProperty(subElement, (Value) null);
                return null;
            }

            @Override
            JSONProperty create(Node node, String subElement, JSONProperty childData) throws RepositoryException {
                final Object value = childData.getValue();
                final String[] stringValue = childData.isMultiple() ? (String[]) value : new String[]{(String) value};
                final Property property = node.setProperty(subElement, stringValue);
                return new JSONProperty(property);
            }
        });
        accessors.put(CHILDREN, new ElementAccessor<JSONChildren, JSONNode>() {
            @Override
            JSONChildren getSubElementContainer(Node node) throws RepositoryException {
                return new JSONChildren(getParentFrom(node), node);
            }

            @Override
            JSONNode getSubElement(Node node, String subElement) throws RepositoryException {
                return new JSONNode(node.getNode(subElement), 1);
            }

            @Override
            JSONNode delete(Node node, String subElement) throws RepositoryException {
                final Node child = node.getNode(subElement);
                child.remove();
                return null;
            }

            @Override
            JSONNode create(Node node, String subElement, JSONNode childData) throws RepositoryException {
                final Node child = node.addNode(subElement);
                // todo: copy data from childData to child
                return new JSONNode(child, 1);
            }
        });
        accessors.put(MIXINS, new ElementAccessor<JSONMixins, JSONMixin>() {
            @Override
            JSONMixins getSubElementContainer(Node node) throws RepositoryException {
                return new JSONMixins(getParentFrom(node), node);
            }

            @Override
            JSONMixin getSubElement(Node node, String subElement) throws RepositoryException {
                return getSubElementContainer(node).getMixins().get(subElement);
            }

            @Override
            JSONMixin delete(Node node, String subElement) throws RepositoryException {
                node.removeMixin(subElement);
                return null;
            }

            @Override
            JSONMixin create(Node node, String subElement, JSONMixin childData) throws RepositoryException {
                node.addMixin(subElement);

                final NodeType[] mixinNodeTypes = node.getMixinNodeTypes();
                NodeType mixin = null;
                for (NodeType mixinNodeType : mixinNodeTypes) {
                    if (mixinNodeType.getName().equals(subElement)) {
                        mixin = mixinNodeType;
                        break;
                    }
                }

                return new JSONMixin(getSubElementContainer(node), mixin);
            }
        });
        accessors.put(VERSIONS, new ElementAccessor<JSONVersions, JSONVersion>() {
            @Override
            JSONVersions getSubElementContainer(Node node) throws RepositoryException {
                return new JSONVersions(getParentFrom(node), node);
            }

            @Override
            JSONVersion getSubElement(Node node, String subElement) throws RepositoryException {
                return null; // todo
            }

            @Override
            JSONVersion delete(Node node, String subElement) throws RepositoryException {
                return null; // todo
            }

            @Override
            JSONVersion create(Node node, String subElement, JSONVersion childData) throws RepositoryException {
                return null; // todo
            }
        });
        accessors.put("", new ElementAccessor<JSONSubElementContainer, JSONNode>() {
            @Override
            Object getElement(Node node, String subElement) throws RepositoryException {
                return new JSONNode(node, 1);
            }

            @Override
            JSONSubElementContainer getSubElementContainer(Node node) throws RepositoryException {
                throw new IllegalStateException("Cannot call getSubElementContainer on identity ElementAccessor");
            }

            @Override
            JSONNode getSubElement(Node node, String subElement) throws RepositoryException {
                throw new IllegalStateException("Cannot call getSubElement on identity ElementAccessor");
            }

            @Override
            JSONNode delete(Node node, String subElement) throws RepositoryException {
                throw new IllegalStateException("Cannot call delete on identity ElementAccessor"); // todo?
            }

            @Override
            JSONNode create(Node node, String subElement, JSONNode childData) throws RepositoryException {
                throw new IllegalStateException("Cannot call create on identity ElementAccessor"); // todo?
            }
        });
    }

    private SpringBeansAccess beansAccess = SpringBeansAccess.getInstance();

    void setBeansAccess(SpringBeansAccess beansAccess) {
        this.beansAccess = beansAccess;
    }

    @GET
    @Path("/version")
    @Produces(MediaType.TEXT_PLAIN)
    public String version() {
        return VERSION;
    }

    @GET
    @Path("/nodes")
    @Produces(MediaType.APPLICATION_JSON)
    /**
     * Needed to get URI without trailing / to work :(
     */
    public Object getRootNode(@Context UriInfo context) throws RepositoryException {
        return perform("", "", "", context, "read", null);
    }

    @GET
    @Path("/nodes/{id: [^/]*}{subElementType: (/(" + API.CHILDREN +
            "|" + API.MIXINS +
            "|" + API.PROPERTIES +
            "|" + API.VERSIONS +
            "))?}{subElement: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Object getNodeById(@PathParam("id") String id, @PathParam("subElementType") String subElementType,
                              @PathParam("subElement") String subElement, @Context UriInfo context)
            throws RepositoryException {
        return perform(id, subElementType, subElement, context, "read", null);
    }

    private Object perform(String idOrPath, String subElementType, String subElement, UriInfo context,
                           String operation, JSONLinkable data) throws RepositoryException {
        return perform(idOrPath, subElementType, subElement, context, operation, data, NodeAccessor.byId);
    }

    private Object perform(String idOrPath, String subElementType, String subElement, UriInfo context,
                           String operation, JSONLinkable data, NodeAccessor nodeAccessor) throws RepositoryException {
        final Session session = beansAccess.getRepository().login(new SimpleCredentials("root", new char[]{'r', 'o',
                'o', 't', '1', '2', '3', '4'}));

        try {
            // check if we're trying to access root's sub-elements
            if (subElementType.isEmpty() && accessors.containsKey(idOrPath)) {
                subElementType = idOrPath;
                idOrPath = "";
            }

            final Node node = nodeAccessor.getNode(idOrPath, session);

            if (subElementType.startsWith("/")) {
                subElementType = subElementType.substring(1);
            }

            if (subElement.startsWith("/")) {
                subElement = subElement.substring(1);
            }

            final ElementAccessor accessor = accessors.get(subElementType);
            if (accessor != null) {
                final Response response = accessor.perform(node, URIUtils.unescape(subElement), operation, data,
                        context);

                session.save();

                return response;
            } else {
                return null;
            }

        } finally {
            session.logout();
        }
    }

    private static interface NodeAccessor {
        Node getNode(String idOrPath, Session session) throws RepositoryException;

        NodeAccessor byId = new NodeAccessor() {
            @Override
            public Node getNode(String idOrPath, Session session) throws RepositoryException {
                return idOrPath.isEmpty() ? session.getRootNode() : session.getNodeByIdentifier(idOrPath);
            }
        };

        NodeAccessor byPath = new NodeAccessor() {
            @Override
            public Node getNode(String idOrPath, Session session) throws RepositoryException {
                return idOrPath.isEmpty() ? session.getRootNode() : session.getNode(idOrPath);
            }
        };
    }

    @PUT
    @Path("/nodes/{id: [^/]*}{subElementType: (/(" + API.CHILDREN +
            "|" + API.MIXINS +
            "|" + API.PROPERTIES +
            "|" + API.VERSIONS +
            "))?}{subElement: .*}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Object createChildNode(@PathParam("id") String id, @PathParam("subElementType") String subElementType,
                                  @PathParam("subElement") String subElement, JSONNode childData, @Context UriInfo context)
            throws RepositoryException {
        return perform(id, subElementType, subElement, context, "create", childData);
    }

    @DELETE
    @Path("/nodes/{id: [^/]*}{subElementType: (/(" + API.CHILDREN +
            "|" + API.MIXINS +
            "|" + API.PROPERTIES +
            "|" + API.VERSIONS +
            "))?}{subElement: .*}")
    public Object deleteNode(@PathParam("id") String id, @PathParam("subElementType") String subElementType,
                             @PathParam("subElement") String subElement, @Context UriInfo context) throws RepositoryException {
        return perform(id, subElementType, subElement, context, "delete", null);
    }

    @GET
    @Path("/byPath{path: /.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Object getByPath(@PathParam("path") String path, @Context UriInfo context) throws RepositoryException {
        int index = 0;
        final List<PathSegment> segments = context.getPathSegments();
        for (PathSegment segment : segments) {
            // first path segment corresponds to the resource mapping so we ignore it
            // and second path corresponds to byPath so we ignore it as well
            String subElementType = segment.getPath();
            if (index > 1) {

                // check if segment is a sub-element marker
                ElementAccessor accessor = accessors.get(subElementType);
                if (accessor != null) {
                    String nodePath = computePathUpTo(segments, index);
                    String subElement = getSubElement(segments, index);
                    return perform(nodePath, subElementType, subElement, context, "read", null, NodeAccessor.byPath);
                }
            }
            index++;
        }

        return perform(computePathUpTo(segments, segments.size()), "", "", context, "read", null, NodeAccessor.byPath);
    }

    private static String computePathUpTo(List<PathSegment> segments, int index) {
        StringBuilder path = new StringBuilder(30 * index);
        // first path segment corresponds to the resource mapping so we ignore it
        // and second path corresponds to byPath so we ignore it as well
        for (int i = 2; i < index; i++) {
            path.append("/").append(segments.get(i).getPath());
        }
        return path.toString();
    }

    private static String getSubElement(List<PathSegment> segments, int index) {
        final int next = index + 1;
        if (next < segments.size()) {
            return segments.get(next).getPath();
        } else {
            return "";
        }
    }
}
