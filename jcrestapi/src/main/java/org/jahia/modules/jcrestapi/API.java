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
import org.jahia.modules.jcrestapi.model.JSONLinkable;
import org.jahia.modules.jcrestapi.model.JSONMixin;
import org.jahia.modules.jcrestapi.model.JSONNode;
import org.jahia.services.content.JCRSessionFactory;
import org.osgi.service.component.annotations.Component;

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
        accessors.put(CHILDREN, new NodeElementAccessor());
        accessors.put(MIXINS, new MixinElementAccessor());
        accessors.put(VERSIONS, new VersionElementAccessor());
        accessors.put("", new IdentityElementAccessor());
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
        return perform("", "", "", context, READ, null);
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
        return perform(id, subElementType, subElement, context, READ, null);
    }

    private Object perform(String idOrPath, String subElementType, String subElement, UriInfo context,
                           String operation, JSONLinkable data) throws RepositoryException {
        return perform(idOrPath, subElementType, subElement, context, operation, data, NodeAccessor.byId);
    }

    private Object perform(String idOrPath, String subElementType, String subElement, UriInfo context,
                           String operation, JSONLinkable data, NodeAccessor nodeAccessor) throws RepositoryException {
        return perform(context, operation, data, nodeAccessor, new ElementsProcessor(idOrPath, subElementType, subElement));
    }

    private Object perform(UriInfo context, String operation, JSONLinkable data, NodeAccessor nodeAccessor, ElementsProcessor processor) throws RepositoryException {
        final Session session = getSession();

        try {
            final Node node = nodeAccessor.getNode(processor.getIdOrPath(), session);

            final ElementAccessor accessor = accessors.get(processor.getSubElementType());
            if (accessor != null) {
                final Response response = accessor.perform(node, processor.getSubElement(), operation, data, context);

                session.save();

                return response;
            } else {
                return null;
            }
        } finally {
            session.logout();
        }
    }

    private Session getSession() throws RepositoryException {
        final Repository repository = beansAccess.getRepository();
        final Session session;
        if (repository instanceof JCRSessionFactory) {
            JCRSessionFactory factory = (JCRSessionFactory) repository;
            session = factory.getCurrentUserSession("live", Locale.ENGLISH);
        } else {
            session = repository.login(getRoot());
        }

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
    @Path("/nodes/{id: [^/]*}{subElementType: (/(" + API.CHILDREN +
            "|" + API.MIXINS +
            "|" + API.PROPERTIES +
            "|" + API.VERSIONS +
            "))?}{subElement: .*}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Object createOrUpdateChildNode(@PathParam("id") String id, @PathParam("subElementType") String subElementType,
                                          @PathParam("subElement") String subElement, JSONNode childData, @Context UriInfo context)
            throws RepositoryException {
        ElementsProcessor processor = new ElementsProcessor(id, subElementType, subElement);
        subElementType = processor.getSubElementType();
        id = processor.getIdOrPath();
        subElement = processor.getSubElement();

        if (childData != null && MIXINS.equals(subElementType)) {
            // initialize mixin from childData: we only need to get its name to create it
            final JSONMixin data = new JSONMixin();
            data.setName(subElement);

            final Session session = getSession();
            try {

                final Node node = NodeAccessor.byId.getNode(id, session);

                final ElementAccessor accessor = accessors.get(subElementType);
                if (accessor != null) {
                    // this creates the mixin object but more importantly adds the mixin to the parent node
                    final Response response = accessor.perform(node, subElement, CREATE_OR_UPDATE, data, context);

                    // we now need to use the rest of the given child data to add / update the parent node content
                    NodeElementAccessor.initNodeFrom(node, childData);

                    session.save();

                    return response;
                } else {
                    return null;
                }
            } finally {
                session.logout();
            }
        }

        return perform(context, CREATE_OR_UPDATE, childData, NodeAccessor.byId, processor);
    }

    @DELETE
    @Path("/nodes/{id: [^/]*}{subElementType: (/(" + API.CHILDREN +
            "|" + API.MIXINS +
            "|" + API.PROPERTIES +
            "|" + API.VERSIONS +
            "))?}{subElement: .*}")
    public Object deleteNode(@PathParam("id") String id, @PathParam("subElementType") String subElementType,
                             @PathParam("subElement") String subElement, @Context UriInfo context) throws RepositoryException {
        return perform(id, subElementType, subElement, context, DELETE, null);
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
                    return perform(nodePath, subElementType, subElement, context, READ, null, NodeAccessor.byPath);
                }
            }
            index++;
        }

        return perform(computePathUpTo(segments, segments.size()), "", "", context, READ, null, NodeAccessor.byPath);
    }

    @GET
    @Path("/byType/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    public Object getByType(@PathParam("type") String type,
                            @QueryParam("named") String name,
                            @QueryParam("orderBy") String orderBy,
                            @QueryParam("limit") int limit,
                            @QueryParam("offset") int offset,
                            @QueryParam("depth") int depth,
                            @Context UriInfo context)
            throws RepositoryException {
        final Session session = getSession();

        try {
            final QueryObjectModelFactory qomFactory = session.getWorkspace().getQueryManager().getQOMFactory();
            final ValueFactory valueFactory = session.getValueFactory();
            final Selector selector = qomFactory.selector(URIUtils.unescape(type), SELECTOR_NAME);

            // hardcode constraint on language for now: either jcr:language doesn't exist or jcr:language is "en"
            Constraint constraint = qomFactory.or(
                    qomFactory.not(qomFactory.propertyExistence(SELECTOR_NAME, Constants.JCR_LANGUAGE)),
                    stringComparisonConstraint(qomFactory.propertyValue(SELECTOR_NAME, Constants.JCR_LANGUAGE), "en", qomFactory, valueFactory)
            );

            // if we have passed a "named" query parameter, only return nodes with the specified name
            if (exists(name)) {
                constraint = qomFactory.or(constraint, stringComparisonConstraint(qomFactory.nodeLocalName(SELECTOR_NAME), name, qomFactory, valueFactory));
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
        } finally {
            session.logout();
        }
    }

    private Comparison stringComparisonConstraint(DynamicOperand operand, String valueOperandShouldBe, QueryObjectModelFactory qomFactory, ValueFactory valueFactory) throws RepositoryException {
        return qomFactory.comparison(operand, QueryObjectModelFactory.JCR_OPERATOR_EQUAL_TO, qomFactory.literal(valueFactory.createValue(valueOperandShouldBe,
                PropertyType.STRING)));
    }

    private SimpleCredentials getRoot() {
        return new SimpleCredentials("root", new char[]{'r', 'o', 'o', 't', '1', '2', '3', '4'});
    }

    private boolean exists(String name) {
        return name != null && !name.isEmpty();
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
