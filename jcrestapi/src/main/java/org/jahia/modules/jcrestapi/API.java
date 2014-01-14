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

import org.jahia.modules.jcrestapi.json.*;
import org.jahia.modules.jcrestapi.path.AccessorPair;
import org.jahia.modules.jcrestapi.path.ItemAccessor;
import org.jahia.modules.jcrestapi.path.NodeAccessor;
import org.jahia.modules.jcrestapi.path.PathParser;
import org.osgi.service.component.annotations.Component;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
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
        });
        accessors.put("", new ElementAccessor<JSONSubElement, JSONNode>() {
            @Override
            JSONSubElement getSubElementContainer(Node node) throws RepositoryException {
                throw new IllegalArgumentException("Cannot get a sub-element container for root node!");
            }

            @Override
            JSONNode getSubElement(Node node, String subElement) throws RepositoryException {
                if (!subElement.isEmpty()) {
                    throw new IllegalArgumentException("Shouldn't have been called on root node accessor. Asked sub-element: "
                            + subElement);
                }
                return new JSONNode(node, 1);
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
    @Produces(MediaType.APPLICATION_JSON)
    /**
     * Needed to get URI without trailing / to work :(
     */
    public Object getRootNode(@Context UriInfo info) throws RepositoryException {
        NodeAccessor.ROOT_ACCESSOR.initWith("/", info.getRequestUriBuilder());
        final Object node = getJSON(NodeAccessor.ROOT_ACCESSOR, ItemAccessor.IDENTITY_ACCESSOR);
        return node;
    }

    @GET
    @Path("/nodes/{id: [^/]*}{subElementType: (/(" + API.CHILDREN +
            "|" + API.MIXINS +
            "|" + API.PROPERTIES +
            "|" + API.VERSIONS +
            "))?}{subElement: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Object getNodeById(@PathParam("id") String id, @PathParam("subElementType") String subElementType,
                              @PathParam("subElement") String subElement, @Context UriInfo info)
            throws
            RepositoryException {
        final Session session = beansAccess.getRepository().login(new SimpleCredentials("root", new char[]{'r', 'o',
                'o', 't', '1', '2', '3', '4'}));

        try {
            // check if we're trying to access root's sub-elements
            if (accessors.containsKey(id)) {
                subElementType = id;
                id = "";
            }

            final Node node = getNode(id, session);

            if (subElementType.startsWith("/")) {
                subElementType = subElementType.substring(1);
            }

            if (subElement.startsWith("/")) {
                subElement = subElement.substring(1);
            }

            final ElementAccessor accessor = accessors.get(subElementType);
            return accessor == null ? null : accessor.getElement(node, URIUtils.unescape(subElement));

        } finally {
            session.logout();
        }
    }

    private Node getNode(String id, Session session) throws RepositoryException {
        return id.isEmpty() ? session.getRootNode() : session.getNodeByIdentifier(id);
    }

    @PUT
    @Path("/nodes/{id}/{child}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Object createChildNode(@PathParam("id") String id, @PathParam("child") String child, JSONNode childData,
                                  @Context UriInfo context) throws RepositoryException {
        final Session session = beansAccess.getRepository().login(new SimpleCredentials("root", new char[]{'r', 'o',
                'o', 't', '1', '2', '3', '4'}));
        try {
            Node node = getNode(id, session);

            Node newNode = node.addNode(child);
            final JSONNode entity = new JSONNode(newNode, 0);

            session.save();

            return Response.created(context.getAbsolutePath()).entity(entity).build();

        } finally {
            session.logout();
        }
    }

    @DELETE
    @Path("/nodes/{id: [^/]*}{subElementType: (/(" + API.CHILDREN +
            "|" + API.MIXINS +
            "|" + API.PROPERTIES +
            "|" + API.VERSIONS +
            "))?}{subElement: .*}")
    public Object deleteNode(@PathParam("id") String id, @Context UriInfo context) throws RepositoryException {
        final Session session = beansAccess.getRepository().login(new SimpleCredentials("root", new char[]{'r', 'o',
                'o', 't', '1', '2', '3', '4'}));
        try {
            Node node = getNode(id, session);

            node.remove();

            session.save();

            return Response.noContent().build();

        } finally {
            session.logout();
        }
    }

    /*@PUT
    @Path("/nodes/{id}/mixins/{mixinName}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createMixin(@PathParam("id") String id, @PathParam("mixinName") String mixinName,
                                @Context UriInfo info) throws RepositoryException, URISyntaxException {
        final Session session = beansAccess.getRepository().login(new SimpleCredentials("root", new char[]{'r', 'o',
                'o', 't', '1', '2', '3', '4'}));
        try {
            Node node = id.isEmpty() ? session.getRootNode() : session.getNodeByIdentifier(id);
            return Response.created(info.getAbsolutePath()).build();

        } finally {
            session.logout();
        }
    }

    @Path("/nodes/{id}/" + API.MIXINS)
    public JSONMixins getMixins(@PathParam("id") String id) throws RepositoryException {
        final Session session = beansAccess.getRepository().login(new SimpleCredentials("root", new char[]{'r', 'o',
                'o', 't', '1', '2', '3', '4'}));
        try {
            Node node = id.isEmpty() ? session.getRootNode() : session.getNodeByIdentifier(id);
            final JSONNode parent = new JSONNode(node, 0);
            return new JSONMixins(parent, node);

        } finally {
            session.logout();
        }
    }*/


    @GET
    @Path("{path: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Object getNode(@PathParam("path") String path, @Context UriInfo info) throws RepositoryException {
        final AccessorPair accessors = PathParser.getAccessorsForPath(info.getBaseUriBuilder(), info.getPathSegments());

        final Object node = getJSON(accessors.nodeAccessor, accessors.itemAccessor);
        return node;
    }

    private Object getJSON(NodeAccessor nodeAccessor, ItemAccessor itemAccessor) throws RepositoryException {
        final Session session = beansAccess.getRepository().login(new SimpleCredentials("root", new char[]{'r', 'o',
                'o', 't', '1', '2', '3', '4'}));
        try {
            // todo: optimize: we shouldn't need to load the whole node if we only want part of it
            final JSONNode node = new JSONNode(nodeAccessor.getNode(session), 1);
            return itemAccessor.getItem(node);
        } finally {
            session.logout();
        }
    }
}
