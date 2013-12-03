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

import org.jahia.modules.jcrestapi.json.JSONItem;
import org.jahia.modules.jcrestapi.json.JSONNode;
import org.jahia.modules.jcrestapi.json.JSONProperty;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.util.Properties;

/**
 * @author Christophe Laprun
 */
@Path(API.API_PATH)
@Produces({MediaType.APPLICATION_JSON, "application/hal+json"})
public class API {
    public static final String VERSION;

    static final String API_PATH = "/api";

    static {
        Properties props = new Properties();
        try {
            props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("jcrestapi.properties"));
        } catch (IOException e) {
            throw new RuntimeException("Could not load jcrestapi.properties.");
        }

        VERSION = props.getProperty("jcrestapi.version");
    }

    private Repository repository;

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    @GET
    @Path("/version")
    @Produces(MediaType.TEXT_PLAIN)
    public String version() {
        return VERSION;
    }

    @GET
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    public JSONNode getRootNode(@Context UriInfo info) throws RepositoryException {
        return getJSON(NodeAccessor.ROOT_ACCESSOR, ItemAccessor.IDENTITY_ACCESSOR, info.getAbsolutePath());
    }

    @GET
    @Path("/properties/{property: .*}")
    @Consumes(MediaType.APPLICATION_JSON)
    public JSONProperty getRootProperty(@PathParam("property") String property, @Context UriInfo info) throws
            RepositoryException {
        return getJSON(NodeAccessor.ROOT_ACCESSOR, new PropertyAccessor(property), info.getAbsolutePath());
    }

    @GET
    @Path("/{path:.*}")
    @Consumes(MediaType.APPLICATION_JSON)
    public JSONNode getNode(@PathParam("path") String path, @Context UriInfo info) throws RepositoryException {
        return getJSON(new PathNodeAccessor(path), ItemAccessor.IDENTITY_ACCESSOR, info.getAbsolutePath());
    }

    @GET
    @Path("/{path: .*}/properties/{property: .*}")
    @Consumes(MediaType.APPLICATION_JSON)
    public JSONProperty getProperty(@PathParam("path") String path, @PathParam("property") String property,
                                    @Context UriInfo info) throws
            RepositoryException {
        return getJSON(new PathNodeAccessor(path), new PropertyAccessor(property), info.getAbsolutePath());
    }

    private <T extends JSONItem> T getJSON(NodeAccessor nodeAccessor, ItemAccessor<T> itemAccessor,
                                           URI uri) throws RepositoryException {
        final Session session = repository.login();
        try {
            final JSONNode node = new JSONNode(nodeAccessor.getNode(session), uri, 1);
            return itemAccessor.getItem(node);
        } finally {
            session.logout();
        }
    }

    private static interface NodeAccessor {
        Node getNode(Session session) throws RepositoryException;

        final static NodeAccessor ROOT_ACCESSOR = new NodeAccessor() {
            @Override
            public Node getNode(Session session) throws RepositoryException {
                return session.getRootNode();
            }
        };
    }

    private static class PathNodeAccessor implements NodeAccessor {
        private final String path;

        private PathNodeAccessor(String path) {
            this.path = JSONItem.unescape("/" + path);
        }

        @Override
        public Node getNode(Session session) throws RepositoryException {
            return session.getNode(path);
        }
    }

    private static interface ItemAccessor<T extends JSONItem> {
        T getItem(JSONNode parent);

        static final ItemAccessor<JSONNode> IDENTITY_ACCESSOR = new ItemAccessor<JSONNode>() {
            @Override
            public JSONNode getItem(JSONNode parent) {
                return parent;
            }
        };
    }

    private static class PropertyAccessor implements ItemAccessor<JSONProperty> {
        private final String property;

        public PropertyAccessor(String property) {
            this.property = property;
        }

        @Override
        public JSONProperty getItem(JSONNode parent) {
            return parent.getProperty(property);
        }
    }
}
