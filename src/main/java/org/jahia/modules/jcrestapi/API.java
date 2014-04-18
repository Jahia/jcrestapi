/**
 * ==========================================================================================
 * =                        DIGITAL FACTORY v7.0 - Community Distribution                   =
 * ==========================================================================================
 *
 *     Rooted in Open Source CMS, Jahia's Digital Industrialization paradigm is about
 *     streamlining Enterprise digital projects across channels to truly control
 *     time-to-market and TCO, project after project.
 *     Putting an end to "the Tunnel effect", the Jahia Studio enables IT and
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
 *
 * JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION
 * ============================================
 *
 *     Copyright (C) 2002-2014 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==========================================================
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
 *     describing the FLOSS exception, and it is also available here:
 *     http://www.jahia.com/license"
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ==========================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.jcrestapi;

import org.jahia.modules.jcrestapi.accessors.*;
import org.jahia.modules.jcrestapi.model.JSONItem;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.utils.LanguageCodeConverters;
import org.osgi.service.component.annotations.Component;

import javax.inject.Inject;
import javax.jcr.*;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.*;

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

    static final String API_PATH = "/api/jcr/v1";

    public static final String PROPERTIES = "properties";
    public static final String MIXINS = "mixins";
    public static final String CHILDREN = "children";
    public static final String VERSIONS = "versions";
    public static final String TYPE = "type";
    public static final String TARGET = "target";
    public static final String PARENT = "parent";
    public static final String PATH = "path";

    protected final static Map<String, ElementAccessor> ACCESSORS = new HashMap<String, ElementAccessor>(7);

    private static final ThreadLocal<SessionInfo> SESSION_HOLDER = new ThreadLocal<SessionInfo>();

    static {
        Properties props = new Properties();
        try {
            props.load(API.class.getClassLoader().getResourceAsStream("jcrestapi.properties"));
        } catch (Exception e) {
            throw new RuntimeException("Could not load jcrestapi.properties.", e);
        }

        VERSION = "API version: 1\nModule version:" + props.getProperty("jcrestapi.version");

        ACCESSORS.put(PROPERTIES, new PropertyElementAccessor());
        ACCESSORS.put(CHILDREN, new ChildrenElementAccessor());
        ACCESSORS.put(MIXINS, new MixinElementAccessor());
        ACCESSORS.put(VERSIONS, new VersionElementAccessor());
        ACCESSORS.put("", new NodeElementAccessor());
    }

    public static SessionInfo getCurrentSession() {
        return SESSION_HOLDER.get();
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

    public API(String workspace, String language, Repository repository, UriInfo context) {
        this.workspace = workspace;
        this.language = language;
        this.repository = repository;
        if (context != null) {
            URIUtils.setBaseURI(context.getBaseUri().toASCIIString());
        }
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

    /**
     * Retrieves the sub-resources in charge of handling requests accessing resources by their identifiers.
     *
     * @param workspace the JCR workspace that we want to access
     * @param language the language code in which we want to retrieve the data
     * @param context a UriInfo instance, automatically injected, providing context about the request URI
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
     * @param workspace the JCR workspace that we want to access
     * @param language the language code in which we want to retrieve the data
     * @param id the identifier of the parent node which sub-elements we want to delete
     * @param subElementType the type of sub-elements to delete
     * @param subElements a list of sub-elements names to delete
     * @param context a UriInfo instance, automatically injected, providing context about the request URI
     *
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
        SESSION_HOLDER.set(new SessionInfo(session, workspace, language));

        return session;
    }

    protected void closeSession(Session session) {
        if (session != null && session.isLive()) {
            session.logout();
        }

        // reset session holder
        SESSION_HOLDER.remove();
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
