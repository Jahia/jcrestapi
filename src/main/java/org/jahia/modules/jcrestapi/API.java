/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2015 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     "This program is free software; you can redistribute it and/or
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
 *     As a special exception to the terms and conditions of version 2.0 of
 *     the GPL (or any later version), you may redistribute this Program in connection
 *     with Free/Libre and Open Source Software ("FLOSS") applications as described
 *     in Jahia's FLOSS exception. You should have received a copy of the text
 *     describing the FLOSS exception, also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ======================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 *
 *
 * ==========================================================================================
 * =                                   ABOUT JAHIA                                          =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia’s Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to “the Tunnel effect”, the Jahia Studio enables IT and
 *     marketing teams to collaboratively and iteratively build cutting-edge
 *     online business solutions.
 *     These, in turn, are securely and easily deployed as modules and apps,
 *     reusable across any digital projects, thanks to the Jahia Private App Store Software.
 *     Each solution provided by Jahia stems from this overarching vision:
 *     Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 *     Founded in 2002 and headquartered in Geneva, Switzerland,
 *     Jahia Solutions Group has its North American headquarters in Washington DC,
 *     with offices in Chicago, Toronto and throughout Europe.
 *     Jahia counts hundreds of global brands and governmental organizations
 *     among its loyal customers, in more than 20 countries across the globe.
 *
 *     For more information, please visit http://www.jahia.com
 */
package org.jahia.modules.jcrestapi;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jahia.modules.jcrestapi.accessors.ChildrenElementAccessor;
import org.jahia.modules.jcrestapi.accessors.ElementAccessor;
import org.jahia.modules.jcrestapi.accessors.MixinElementAccessor;
import org.jahia.modules.jcrestapi.accessors.NodeElementAccessor;
import org.jahia.modules.jcrestapi.accessors.PropertyElementAccessor;
import org.jahia.modules.jcrestapi.accessors.VersionElementAccessor;
import org.jahia.modules.jcrestapi.json.APIObjectFactory;
import org.jahia.modules.jcrestapi.json.JSONQuery;
import org.jahia.modules.json.JSONConstants;
import org.jahia.modules.json.JSONItem;
import org.jahia.modules.json.JSONNode;
import org.jahia.modules.json.Names;
import org.jahia.modules.json.jcr.SessionAccess;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.utils.LanguageCodeConverters;
import org.osgi.service.component.annotations.Component;

/**
 * The main entry point to the JCR RESTful API.
 *
 * @author Christophe Laprun
 */
@Component
@Path(API.API_PATH)
@Produces({"application/hal+json"})
public class API {
    public static final String SELF = "self";
    public static final String ABSOLUTE = "absolute";
    private static final String VERSION;

    public static final String DELETE = "delete";
    public static final String CREATE_OR_UPDATE = "createOrUpdate";
    public static final String READ = "read";
    public static final String UPLOAD = "upload";
    public static final String AS_JSON_STRING = "asJSONString";

    static final String API_PATH = "/api/jcr/v1";

    public static final String TYPE = "type";
    public static final String TARGET = "target";
    public static final String PARENT = "parent";
    public static final String PATH = "path";
    public static final String NODE_AT_VERSION = "nodeAtVersion";

    public static final String INCLUDE_FULL_CHILDREN = "includeFullChildren";
    public static final String RESOLVE_REFERENCES = "resolveReferences";
    public static final String NO_LINKS = "noLinks";
    public static final String CHILDREN_NODETYPE_FILTER = "childrenNodeTypes";

    private static final ThreadLocal<Boolean> resolveReferences = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };
    private static final ThreadLocal<Boolean> outputLinks = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return true;
        }
    };
    private static final ThreadLocal<Boolean> includeFullChildren = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };
    protected static final Map<String, ElementAccessor> ACCESSORS = new HashMap<String, ElementAccessor>(7);

    static {
        Properties props = new Properties();
        try {
            props.load(API.class.getClassLoader().getResourceAsStream("jcrestapi.properties"));
        } catch (Exception e) {
            throw new RuntimeException("Could not load jcrestapi.properties.", e);
        }

        VERSION = "API version: 1.1\nModule version:" + props.getProperty("jcrestapi.version");

        ACCESSORS.put(JSONConstants.PROPERTIES, new PropertyElementAccessor());
        ACCESSORS.put(JSONConstants.CHILDREN, new ChildrenElementAccessor());
        ACCESSORS.put(JSONConstants.MIXINS, new MixinElementAccessor());
        ACCESSORS.put(JSONConstants.VERSIONS, new VersionElementAccessor());
        ACCESSORS.put("", new NodeElementAccessor());
    }

    @Inject
    private Repository repository;
    protected String workspace;
    protected String language;

    public API() {
    }

    public API(String workspace, String language, Repository repository, UriInfo context) {
        this.workspace = workspace;
        this.language = language;
        this.repository = repository;
        if (context != null) {
            URIUtils.setBaseURI(context.getBaseUri().toASCIIString());
        }
    }

    public static APIObjectFactory getFactory() {
        return APIObjectFactory.getInstance();
    }

    /**
     * Specifies whether the API should resolve node references in properties when generating the node representations. This status is only valid for the current Thread.
     * @param newResolveReferences <code>true</code> if the API should resolve the references in properties, <code>false</code> otherwise
     * @return the status of reference resolving as it was before this method was called
     */
    public static boolean setResolveReferences(boolean newResolveReferences) {
        return setThreadLocalFlag(resolveReferences, newResolveReferences);
    }

    /**
     * Specifies whether the API should generate HATEOAS links in the node representations. This status is only valid for the current Thread.
     * @param newOutputLinks <code>true</code> if the API should output HATEOAS links, <code>false</code> otherwise
     * @return the status of links generation as it was before this method was called
     */
    public static boolean setOutputLinks(boolean newOutputLinks) {
        return setThreadLocalFlag(outputLinks, newOutputLinks);
    }

    /**
     * Specifies whether the API should include full children when generating the node representations. This status is only valid for the current Thread.
     *
     * @param newIncludeFullChildren <code>true</code> if the API should generate a complete representation of children, <code>false</code> otherwise
     * @return the status of the children generation as it was before this method was called
     */
    public static boolean setIncludeFullChildren(boolean newIncludeFullChildren) {
        return setThreadLocalFlag(includeFullChildren, newIncludeFullChildren);
    }

    private static boolean setThreadLocalFlag(ThreadLocal<Boolean> local, boolean newValue) {
        boolean old = local.get();
        local.set(newValue);
        return old;
    }

    /**
     * Returns the current version of the API and of this implementation.
     */
    @GET
    @Path("/version")
    @Produces(MediaType.TEXT_PLAIN)
    public String version() {
        return VERSION;
    }

    @POST
    @Path("/{workspace}/{language}/query")
    @Consumes(MediaType.APPLICATION_JSON)
    public Object query(@PathParam("workspace") String workspace, @PathParam("language") String language, JSONQuery jsonQuery, @Context UriInfo context) {
        if (jsonQuery != null) {
            Session session = null;

            try {
                session = getSession(workspace, language);
                final QueryManager queryManager = session.getWorkspace().getQueryManager();
                final Query query = queryManager.createQuery(jsonQuery.getQuery(), Query.JCR_SQL2);
                if (jsonQuery.getLimit() > 0) {
                    query.setLimit(jsonQuery.getLimit());
                }

                if (jsonQuery.getOffset() > 0) {
                    query.setOffset(jsonQuery.getOffset());
                }

                final QueryResult queryResult = query.execute();

                final NodeIterator nodes = queryResult.getNodes();
                final List<JSONNode> result = new LinkedList<JSONNode>();
                while (nodes.hasNext()) {
                    JSONNode node = getFactory().createNode(nodes.nextNode(), Utils.getFilter(context), 1);
                    result.add(node);
                }

                return Response.ok(result).build();
            } catch (Exception e) {
                throw new APIException(e);
            } finally {
                closeSession(session);
            }
        } else {
            return Response.ok().build();
        }
    }

    /**
     * Retrieves the sub-resources in charge of handling requests accessing resources by their identifiers.
     *
     * @param workspace the JCR workspace that we want to access
     * @param language  the language code in which we want to retrieve the data
     * @param context   a UriInfo instance, automatically injected, providing context about the request URI
     * @return a Nodes instance configured to access JCR data from the specified workspace and language
     */
    @Path("/{workspace}/{language}/" + Nodes.MAPPING)
    public Nodes getNodes(@PathParam("workspace") String workspace, @PathParam("language") String language, @Context UriInfo context) {
        return new Nodes(workspace, language, repository, context);
    }

    /**
     * Retrieves the sub-resources in charge of handling requests accessing resources by their types.
     *
     * @param workspace the JCR workspace that we want to access
     * @param language  the language code in which we want to retrieve the data
     * @param context   a UriInfo instance, automatically injected, providing context about the request URI
     * @return a Types instance configured to access JCR data from the specified workspace and language
     */
    @Path("/{workspace}/{language}/" + Types.MAPPING)
    public Types getByType(@PathParam("workspace") String workspace, @PathParam("language") String language, @Context UriInfo context) {
        return new Types(workspace, language, repository, context);
    }

    /**
     * Retrieves the sub-resources in charge of handling requests accessing resources by their paths.
     *
     * @param workspace the JCR workspace that we want to access
     * @param language  the language code in which we want to retrieve the data
     * @param context   a UriInfo instance, automatically injected, providing context about the request URI
     * @return a Types instance configured to access JCR data from the specified workspace and language
     */
    @Path("/{workspace}/{language}/" + Paths.MAPPING)
    public Paths getByPath(@PathParam("workspace") String workspace, @PathParam("language") String language, @Context UriInfo context) {
        return new Paths(workspace, language, repository, context);
    }

    protected Response perform(String workspace, String language, String idOrPath, String subElementType, String subElement, UriInfo context,
                               String operation, JSONItem data) {
        return perform(workspace, language, idOrPath, subElementType, subElement, context, operation, data, NodeAccessor.BY_ID);
    }

    /**
     * Performs a batch delete of all specified sub-element types identified by the given list of sub-elements. Note that this method could actually
     * be extended to include other types of batch operations.
     *
     * @param workspace      the JCR workspace that we want to access
     * @param language       the language code in which we want to retrieve the data
     * @param id             the identifier of the parent node which sub-elements we want to delete
     * @param subElementType the type of sub-elements to delete
     * @param subElements    a list of sub-elements names to delete
     * @param context        a UriInfo instance, automatically injected, providing context about the request URI
     * @return a Response ready to be sent to the client
     */
    protected Response performBatchDelete(String workspace, String language, String id, String subElementType, List<String> subElements, UriInfo context) {
        Session session = null;

        try {
            session = getSession(workspace, language);

            // process given elements
            final ElementsProcessor processor = new ElementsProcessor(id, subElementType, "");
            id = processor.getIdOrPath();
            subElementType = processor.getSubElementType();

            final Node node = NodeAccessor.BY_ID.getNode(id, session);

            final ElementAccessor accessor = ACCESSORS.get(subElementType);
            if (accessor != null) {
                final Response response = accessor.perform(node, subElements, DELETE, null, context);

                session.save();

                return response;
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new APIException(e, DELETE, NodeAccessor.BY_ID.getType(), id, subElementType, subElements, null);
        } finally {
            closeSession(session);
        }
    }

    protected Response perform(String workspace, String language, String idOrPath, String subElementType, String subElement, UriInfo context,
                               String operation, JSONItem data, NodeAccessor nodeAccessor) {
        return perform(workspace, language, context, operation, data, nodeAccessor, new ElementsProcessor(idOrPath, subElementType, subElement));
    }

    protected Response perform(String workspace, String language, UriInfo context, String operation, JSONItem data, NodeAccessor nodeAccessor, ElementsProcessor processor) {
        Session session = null;

        resolveReferences.set(Utils.getFlagValueFrom(context, RESOLVE_REFERENCES));
        outputLinks.set(!Utils.getFlagValueFrom(context, NO_LINKS));
        includeFullChildren.set(Utils.getFlagValueFrom(context, INCLUDE_FULL_CHILDREN));

        final String idOrPath = processor.getIdOrPath();
        final String subElementType = processor.getSubElementType();
        final String subElement = processor.getSubElement();

        try {
            session = getSession(workspace, language);

            final Node node = nodeAccessor.getNode(idOrPath, session);

            final ElementAccessor accessor = ACCESSORS.get(subElementType);
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
            resolveReferences.set(false);
            outputLinks.set(true);
            includeFullChildren.set(false);
            closeSession(session);
        }
    }

    protected Session getSession(String workspace, String language) throws RepositoryException {
        if (!Utils.exists(workspace)) {
            workspace = "default";
        }

        if (!Utils.exists(language)) {
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
        SessionAccess.setCurrentSession(session, workspace, language);

        return session;
    }

    protected void closeSession(Session session) {
        if (session != null && session.isLive()) {
            session.logout();
        }

        // reset session holder
        SessionAccess.closeCurrentSession();
    }

    public static boolean shouldResolveReferences() {
        return resolveReferences.get();
    }

    public static boolean shouldOutputLinks() {
        return outputLinks.get();
    }

    public static boolean shouldIncludeFullChildren() {
        return includeFullChildren.get();
    }

    protected static interface NodeAccessor {
        Node getNode(String idOrPath, Session session) throws RepositoryException;

        String getType();

        NodeAccessor BY_ID = new NodeAccessor() {
            private static final String TYPE = "byId";

            @Override
            public Node getNode(String idOrPath, Session session) throws RepositoryException {
                return idOrPath.isEmpty() ? session.getRootNode() : session.getNodeByIdentifier(idOrPath);
            }

            @Override
            public String getType() {
                return TYPE;
            }
        };

        NodeAccessor BY_PATH = new NodeAccessor() {
            private static final String TYPE = "byPath";

            @Override
            public Node getNode(String idOrPath, Session session) throws RepositoryException {
                return idOrPath.isEmpty() ? session.getRootNode() : session.getNode(idOrPath);
            }

            @Override
            public String getType() {
                return TYPE;
            }
        };
    }

    protected static class ElementsProcessor {
        private final String idOrPath;
        private final String subElementType;
        private final String subElement;

        public ElementsProcessor(String idOrPath, String subElementType, String subElement) {
            // check if we're trying to access root's sub-elements
            if (subElementType.isEmpty() && ACCESSORS.containsKey(idOrPath)) {
                subElementType = idOrPath;
                idOrPath = "";
            }

            if (subElementType.startsWith("/")) {
                subElementType = subElementType.substring(1);
            }

            if (subElement.startsWith("/")) {
                subElement = subElement.substring(1);
            }

            this.idOrPath = Names.unescape(idOrPath);
            this.subElementType = subElementType;
            this.subElement = Names.unescape(subElement);
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
