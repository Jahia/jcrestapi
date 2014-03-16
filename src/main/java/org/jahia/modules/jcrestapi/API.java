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

import org.jahia.modules.jcrestapi.accessors.*;
import org.jahia.modules.jcrestapi.model.JSONItem;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.utils.LanguageCodeConverters;
import org.osgi.service.component.annotations.Component;

import javax.inject.Inject;
import javax.jcr.*;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.*;

/**
 * @author Christophe Laprun
 */
@Component
@Path(API.API_PATH)
@Produces({MediaType.APPLICATION_JSON})
public class API {
    public static final String VERSION;
    public static final String DELETE = "delete";
    public static final String CREATE_OR_UPDATE = "createOrUpdate";
    public static final String READ = "read";

    static final String API_PATH = "/api";

    public static final String PROPERTIES = "properties";
    public static final String MIXINS = "mixins";
    public static final String CHILDREN = "children";
    public static final String VERSIONS = "versions";
    public static final String TYPE = "type";
    public static final String TARGET = "target";
    public static final String PARENT = "parent";
    public static final String PATH = "path";

    protected final static Map<String, ElementAccessor> accessors = new HashMap<String, ElementAccessor>(7);

    static {
        Properties props = new Properties();
        try {
            props.load(API.class.getClassLoader().getResourceAsStream("jcrestapi.properties"));
        } catch (Exception e) {
            throw new RuntimeException("Could not load jcrestapi.properties.");
        }

        VERSION = props.getProperty("jcrestapi.version");

        accessors.put(PROPERTIES, new PropertyElementAccessor());
        accessors.put(CHILDREN, new ChildrenElementAccessor());
        accessors.put(MIXINS, new MixinElementAccessor());
        accessors.put(VERSIONS, new VersionElementAccessor());
        accessors.put("", new NodeElementAccessor());
    }

    private static final ThreadLocal<SessionInfo> sessionHolder = new ThreadLocal<SessionInfo>();

    public static SessionInfo getCurrentSession() {
        return sessionHolder.get();
    }

    public static class SessionInfo {
        public final Session session;
        public final String workspace;
        public final String language;

        public SessionInfo(Session session, String workspace, String language) {
            this.session = session;
            this.workspace = workspace;
            this.language = language;
        }
    }

    @Inject
    private Repository repository;
    protected String workspace;
    protected String language;

    public API() {
    }

    public API(String workspace, String language) {
        this.workspace = workspace;
        this.language = language;
    }

    @GET
    @Path("/version")
    @Produces(MediaType.TEXT_PLAIN)
    public String version() {
        return VERSION;
    }

    @Path("/{workspace}/{language}/" + Nodes.MAPPING)
    public Nodes getNodes(@PathParam("workspace") String workspace, @PathParam("language") String language, @Context UriInfo context) {
        final Nodes nodes = new Nodes(workspace, language);
        nodes.initRepositoryWith(repository);
        return nodes;
    }

    @Path("/{workspace}/{language}/" + Types.MAPPING)
    public Types getByType(@PathParam("workspace") String workspace, @PathParam("language") String language, @Context UriInfo context) {
        final Types byType = new Types(workspace, language);
        byType.initRepositoryWith(repository);
        return byType;
    }

    @Path("/{workspace}/{language}/" + Paths.MAPPING)
    public Paths getByPath(@PathParam("workspace") String workspace, @PathParam("language") String language, @Context UriInfo context) {
        final Paths byPath = new Paths(workspace, language);
        byPath.initRepositoryWith(repository);
        return byPath;
    }

    protected void initRepositoryWith(Repository repository) {
        this.repository = repository;
    }

    @POST
    @Path("/{workspace}/{language}/rename/{id}/to/{newName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Object renameNode(@PathParam("workspace") String workspace,
                             @PathParam("language") String language,
                             @PathParam("id") String id,
                             @PathParam("newName") String newName,
                             @Context UriInfo context) {
        Session session = null;

        try {
            session = getSession(workspace, language);

            final Node node = session.getNodeByIdentifier(id);
            session.move(node.getPath(), node.getParent().getPath() + "/" + newName);

            session.save();

            return ElementAccessor.getSeeOtherResponse(URIUtils.getIdURI(id), context);
        } catch (Exception e) {
            throw new APIException(e);
        } finally {
            closeSession(session);
        }
    }

    public static boolean exists(String name) {
        return name != null && !name.isEmpty();
    }

    protected Object perform(String workspace, String language, String idOrPath, String subElementType, String subElement, UriInfo context,
                             String operation, JSONItem data) {
        return perform(workspace, language, idOrPath, subElementType, subElement, context, operation, data, NodeAccessor.byId);
    }

    protected Object performBatchDelete(String workspace, String language, String idOrPath, String subElementType, List<String> subElements, UriInfo context) {
        Session session = null;

        try {
            session = getSession(workspace, language);

            // process given elements
            final ElementsProcessor processor = new ElementsProcessor(idOrPath, subElementType, "");
            idOrPath = processor.getIdOrPath();
            subElementType = processor.getSubElementType();

            final Node node = NodeAccessor.byId.getNode(idOrPath, session);

            final ElementAccessor accessor = accessors.get(subElementType);
            if (accessor != null) {
                final Response response = accessor.perform(node, subElements, DELETE, null, context);

                session.save();

                return response;
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new APIException(e, DELETE, NodeAccessor.BY_ID, idOrPath, subElementType, subElements, null);
        } finally {
            closeSession(session);
        }
    }

    protected Object perform(String workspace, String language, String idOrPath, String subElementType, String subElement, UriInfo context,
                             String operation, JSONItem data, NodeAccessor nodeAccessor) {
        return perform(workspace, language, context, operation, data, nodeAccessor, new ElementsProcessor(idOrPath, subElementType, subElement));
    }

    protected Object perform(String workspace, String language, UriInfo context, String operation, JSONItem data, NodeAccessor nodeAccessor, ElementsProcessor processor) {
        Session session = null;

        final String idOrPath = processor.getIdOrPath();
        final String subElementType = processor.getSubElementType();
        final String subElement = processor.getSubElement();

        try {
            session = getSession(workspace, language);

            final Node node = nodeAccessor.getNode(idOrPath, session);

            final ElementAccessor accessor = accessors.get(subElementType);
            if (accessor != null) {
                final Response response = accessor.perform(node, subElement, operation, data, context);

                session.save();

                return response;
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new APIException(e, operation, nodeAccessor.getType(), idOrPath, subElementType, Collections.singletonList(subElement), data);
        } finally {
            closeSession(session);
        }
    }

    protected Session getSession(String workspace, String language) throws RepositoryException {
        if (!exists(workspace)) {
            workspace = "default";
        }

        if (!exists(language)) {
            language = "en"; // todo: retrieve configured default language if possible
        }

        final Session session;
        if (repository instanceof JCRSessionFactory) {
            JCRSessionFactory factory = (JCRSessionFactory) repository;
            session = factory.getCurrentUserSession(workspace, LanguageCodeConverters.languageCodeToLocale(language), Locale.ENGLISH);
        } else {
            session = repository.login(new SimpleCredentials("root", new char[]{'r', 'o', 'o', 't', '1', '2', '3', '4'}), workspace);
        }

        // put the session in the session holder so that other objects can access it if needed
        sessionHolder.set(new SessionInfo(session, workspace, language));

        return session;
    }

    protected void closeSession(Session session) {
        if (session != null && session.isLive()) {
            session.logout();
        }

        // reset session holder
        sessionHolder.remove();
    }

    protected static interface NodeAccessor {
        public static final String BY_ID = "byId";
        public static final String BY_PATH = "byPath";

        Node getNode(String idOrPath, Session session) throws RepositoryException;

        String getType();

        NodeAccessor byId = new NodeAccessor() {
            @Override
            public Node getNode(String idOrPath, Session session) throws RepositoryException {
                return idOrPath.isEmpty() ? session.getRootNode() : session.getNodeByIdentifier(idOrPath);
            }

            @Override
            public String getType() {
                return BY_ID;
            }
        };

        NodeAccessor byPath = new NodeAccessor() {
            @Override
            public Node getNode(String idOrPath, Session session) throws RepositoryException {
                return idOrPath.isEmpty() ? session.getRootNode() : session.getNode(idOrPath);
            }

            @Override
            public String getType() {
                return BY_PATH;
            }
        };
    }

    protected static class ElementsProcessor {
        private String idOrPath;
        private String subElementType;
        private String subElement;

        public ElementsProcessor(String idOrPath, String subElementType, String subElement) {
            // check if we're trying to access root's sub-elements
            if (subElementType.isEmpty() && accessors.containsKey(idOrPath)) {
                subElementType = idOrPath;
                idOrPath = "";
            }

            if (subElementType.startsWith("/")) {
                subElementType = subElementType.substring(1);
            }

            if (subElement.startsWith("/")) {
                subElement = subElement.substring(1);
            }

            this.idOrPath = URIUtils.unescape(idOrPath);
            this.subElementType = subElementType;
            this.subElement = URIUtils.unescape(subElement);
        }

        public String getIdOrPath() {
            return idOrPath;
        }

        public String getSubElementType() {
            return subElementType;
        }

        public String getSubElement() {
            return subElement;
        }
    }
}
