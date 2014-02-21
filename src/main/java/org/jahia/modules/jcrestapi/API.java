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

import org.jahia.api.Constants;
import org.jahia.modules.jcrestapi.accessors.*;
import org.jahia.modules.jcrestapi.model.JSONItem;
import org.jahia.modules.jcrestapi.model.JSONNode;
import org.jahia.modules.jcrestapi.model.JSONProperty;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.utils.LanguageCodeConverters;
import org.osgi.service.component.annotations.Component;

import javax.inject.Inject;
import javax.jcr.*;
import javax.jcr.query.QueryResult;
import javax.jcr.query.qom.*;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
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
    private static final String SELECTOR_NAME = "type";
    public static final String TYPE = "type";
    public static final String TARGET = "target";
    public static final String PARENT = "parent";

    private final static Map<String, ElementAccessor> accessors = new HashMap<String, ElementAccessor>(7);

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

    @Inject
    private Repository repository;

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

    @GET
    @Path("/version")
    @Produces(MediaType.TEXT_PLAIN)
    public String version() {
        return VERSION;
    }

    @GET
    @Path("/{workspace}/{language}/nodes")
    @Produces(MediaType.APPLICATION_JSON)
    /**
     * Needed to get URI without trailing / to work :(
     */
    public Object getRootNode(@PathParam("workspace") String workspace,
                              @PathParam("language") String language,
                              @Context UriInfo context) {
        return perform(workspace, language, "", "", "", context, READ, null);
    }

    @GET
    @Path("/{workspace}/{language}/nodes/{id: [^/]*}{subElementType: (/(" + API.CHILDREN +
            "|" + API.MIXINS +
            "|" + API.PROPERTIES +
            "|" + API.VERSIONS +
            "))?}{subElement: .*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Object getNodeById(@PathParam("workspace") String workspace,
                              @PathParam("language") String language,
                              @PathParam("id") String id,
                              @PathParam("subElementType") String subElementType,
                              @PathParam("subElement") String subElement,
                              @Context UriInfo context) {
        return perform(workspace, language, id, subElementType, subElement, context, READ, null);
    }

    private Object perform(String workspace, String language, String idOrPath, String subElementType, String subElement, UriInfo context,
                           String operation, JSONItem data) {
        return perform(workspace, language, idOrPath, subElementType, subElement, context, operation, data, NodeAccessor.byId);
    }

    private Object perform(String workspace, String language, String idOrPath, String subElementType, String subElement, UriInfo context,
                           String operation, JSONItem data, NodeAccessor nodeAccessor) {
        return perform(workspace, language, context, operation, data, nodeAccessor, new ElementsProcessor(idOrPath, subElementType, subElement));
    }

    private Object perform(String workspace, String language, UriInfo context, String operation, JSONItem data, NodeAccessor nodeAccessor, ElementsProcessor processor) {
        Session session = null;

        try {
            session = getSession(workspace, language);
            final Node node = nodeAccessor.getNode(processor.getIdOrPath(), session);

            final ElementAccessor accessor = accessors.get(processor.getSubElementType());
            if (accessor != null) {
                final Response response = accessor.perform(node, processor.getSubElement(), operation, data, context);

                session.save();

                return response;
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new APIException(e);
        } finally {
            if (session != null) {
                session.logout();
            }

            // reset session holder
            sessionHolder.remove();
        }
    }

    private Session getSession(String workspace, String language) throws RepositoryException {
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
            session = repository.login(getRoot(), workspace);
        }

        // put the session in the session holder so that other objects can access it if needed
        sessionHolder.set(new SessionInfo(session, workspace, language));

        return session;
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
    @Path("/{workspace}/{language}/nodes/{id: [^/]*}{subElementType: (/(" + API.CHILDREN +
            "|" + API.MIXINS +
            "))?}{subElement: .*}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Object createOrUpdateChildNode(@PathParam("workspace") String workspace,
                                          @PathParam("language") String language,
                                          @PathParam("id") String id,
                                          @PathParam("subElementType") String subElementType,
                                          @PathParam("subElement") String subElement,
                                          JSONNode childData,
                                          @Context UriInfo context) {
        ElementsProcessor processor = new ElementsProcessor(id, subElementType, subElement);
        return perform(workspace, language, context, CREATE_OR_UPDATE, childData, NodeAccessor.byId, processor);
    }

    @PUT
    @Path("/{workspace}/{language}/nodes/{id: [^/]*}/properties/{subElement}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Object createOrUpdateProperty(@PathParam("workspace") String workspace,
                                         @PathParam("language") String language,
                                         @PathParam("id") String id,
                                         @PathParam("subElement") String subElement,
                                         JSONProperty childData,
                                         @Context UriInfo context) {
        ElementsProcessor processor = new ElementsProcessor(id, PROPERTIES, subElement);
        return perform(workspace, language, context, CREATE_OR_UPDATE, childData, NodeAccessor.byId, processor);
    }

    @DELETE
    @Path("/{workspace}/{language}/nodes/{id: [^/]*}/properties/{subElement}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Object deleteProperty(@PathParam("workspace") String workspace,
                                 @PathParam("language") String language,
                                 @PathParam("id") String id,
                                 @PathParam("subElement") String subElement,
                                 @Context UriInfo context) {
        ElementsProcessor processor = new ElementsProcessor(id, PROPERTIES, subElement);
        return perform(workspace, language, context, DELETE, null, NodeAccessor.byId, processor);
    }


    @DELETE
    @Path("/{workspace}/{language}/nodes/{id: [^/]*}{subElementType: (/(" + API.CHILDREN +
            "|" + API.MIXINS +
            "|" + API.PROPERTIES +
            "))?}{subElement: .*}")
    public Object deleteNode(@PathParam("workspace") String workspace,
                             @PathParam("language") String language,
                             @PathParam("id") String id,
                             @PathParam("subElementType") String subElementType,
                             @PathParam("subElement") String subElement,
                             @Context UriInfo context) {
        return perform(workspace, language, id, subElementType, subElement, context, DELETE, null);
    }

    @GET
    @Path("/{workspace}/{language}/byPath{path: /.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Object getByPath(@PathParam("workspace") String workspace,
                            @PathParam("language") String language,
                            @PathParam("path") String path,
                            @Context UriInfo context) {

        // only consider useful segments, starting after /api/{workspace}/{language}/byPath
        final List<PathSegment> pathSegments = context.getPathSegments();
        final List<PathSegment> usefulSegments = pathSegments.subList(4, pathSegments.size());
        int index = 0;
        for (PathSegment segment : usefulSegments) {
            // check if segment is a sub-element marker
            String subElementType = segment.getPath();
            ElementAccessor accessor = accessors.get(subElementType);
            if (accessor != null) {
                String nodePath = computePathUpTo(usefulSegments, index);
                String subElement = getSubElement(usefulSegments, index);
                return perform(workspace, language, nodePath, subElementType, subElement, context, READ, null, NodeAccessor.byPath);
            }
        }

        return perform(workspace, language, computePathUpTo(usefulSegments, usefulSegments.size()), "", "", context, READ, null, NodeAccessor.byPath);
    }

    @GET
    @Path("/{workspace}/{language}/byType/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    public Object getByType(@PathParam("workspace") String workspace,
                            @PathParam("language") String language,
                            @PathParam("type") String type,
                            @QueryParam("nameContains") List<String> nameConstraints,
                            @QueryParam("orderBy") String orderBy,
                            @QueryParam("limit") int limit,
                            @QueryParam("offset") int offset,
                            @QueryParam("depth") int depth,
                            @Context UriInfo context) {
        Session session = null;

        try {
            session = getSession(workspace, language);
            final QueryObjectModelFactory qomFactory = session.getWorkspace().getQueryManager().getQOMFactory();
            final ValueFactory valueFactory = session.getValueFactory();
            final Selector selector = qomFactory.selector(URIUtils.unescape(type), SELECTOR_NAME);

            // hardcode constraint on language for now: either jcr:language doesn't exist or jcr:language is "en"
            Constraint constraint = qomFactory.or(
                    qomFactory.not(qomFactory.propertyExistence(SELECTOR_NAME, Constants.JCR_LANGUAGE)),
                    stringComparisonConstraint(qomFactory.propertyValue(SELECTOR_NAME, Constants.JCR_LANGUAGE), "en", qomFactory, valueFactory)
            );

            // if we have passed "nameContains" query parameters, only return nodes which name contains the specified terms
            if (nameConstraints != null && !nameConstraints.isEmpty()) {
                for (String name : nameConstraints) {
                    final Comparison likeConstraint = qomFactory.comparison(qomFactory.nodeLocalName(SELECTOR_NAME), QueryObjectModelFactory.JCR_OPERATOR_LIKE,
                            qomFactory.literal(valueFactory.createValue("%" + name + "%", PropertyType.STRING)));
                    constraint = qomFactory.and(constraint, likeConstraint);
                }
            }

            Ordering[] orderings = null;
            // ordering deactivated because it currently doesn't work, probably due to a bug in QueryServiceImpl
            if (exists(orderBy)) {
                if ("desc".equalsIgnoreCase(orderBy)) {
                    orderings = new Ordering[]{qomFactory.descending(qomFactory.nodeLocalName(SELECTOR_NAME))};
                } else {
                    orderings = new Ordering[]{qomFactory.ascending(qomFactory.nodeLocalName(SELECTOR_NAME))};
                }
            }

            final QueryObjectModel query = qomFactory.createQuery(selector, constraint, orderings, new Column[]{qomFactory.column(SELECTOR_NAME, null, null)});
            if (limit > 0) {
                query.setLimit(limit);
            }
            query.setOffset(offset);

            final QueryResult queryResult = query.execute();

            final NodeIterator nodes = queryResult.getNodes();
            final List<JSONNode> result = new LinkedList<JSONNode>();
            while (nodes.hasNext()) {
                result.add(new JSONNode(nodes.nextNode(), depth));
            }

            return Response.ok(result).build();
        } catch (Exception e) {
            throw new APIException(e);
        } finally {
            if (session != null) {
                session.logout();
            }

            // reset session holder
            sessionHolder.remove();
        }
    }

    private Comparison stringComparisonConstraint(DynamicOperand operand, String valueOperandShouldBe, QueryObjectModelFactory qomFactory, ValueFactory valueFactory) throws RepositoryException {
        return qomFactory.comparison(operand, QueryObjectModelFactory.JCR_OPERATOR_EQUAL_TO, qomFactory.literal(valueFactory.createValue(valueOperandShouldBe,
                PropertyType.STRING)));
    }

    private SimpleCredentials getRoot() {
        return new SimpleCredentials("root", new char[]{'r', 'o', 'o', 't', '1', '2', '3', '4'});
    }

    public static boolean exists(String name) {
        return name != null && !name.isEmpty();
    }

    private static String computePathUpTo(List<PathSegment> segments, int index) {
        StringBuilder path = new StringBuilder(30 * index);
        for (int i = 0; i < index; i++) {
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

    private class ElementsProcessor {
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
