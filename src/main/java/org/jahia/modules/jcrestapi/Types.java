/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2017 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
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

import org.jahia.api.Constants;
import org.jahia.modules.json.Filter;
import org.jahia.modules.json.JSONNode;
import org.jahia.modules.json.Names;

import javax.jcr.*;
import javax.jcr.query.QueryResult;
import javax.jcr.query.qom.*;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Christophe Laprun
 */
@Produces({Utils.MEDIA_TYPE_APPLICATION_HAL_PLUS_JSON, MediaType.APPLICATION_JSON})
public class Types extends API {

    private static final String SELECTOR_NAME = "type";
    static final String MAPPING = "types";

    public Types(String workspace, String language, Repository repository, UriInfo context) {
        super(workspace, language, repository, context);
    }

    @GET
    @Path("/{type}")
    @Produces({Utils.MEDIA_TYPE_APPLICATION_HAL_PLUS_JSON, MediaType.APPLICATION_JSON})
    public Object getByType(@PathParam("workspace") String workspace,
                            @PathParam("language") String language,
                            @PathParam("type") String type,
                            @QueryParam("nameContains") List<String> nameConstraints,
                            @QueryParam("orderBy") String orderBy,
                            @QueryParam("limit") int limit,
                            @QueryParam("offset") int offset,
                            @QueryParam("depth") int depth,
                            @Context UriInfo context) {

        if (API.isQueryDisabled()) {
            APIExceptionMapper.LOGGER.debug("Types endpoint is disabled. Attempted query on " + type);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        final String unescapedNodetype = Names.unescape(type);
        if (API.excludedNodeTypes.contains(unescapedNodetype)) {
            return Response.status(Response.Status.FORBIDDEN).entity("'" + unescapedNodetype + "' is not available for querying.").build();
        }

        Session session = null;

        try {

            session = getSession(workspace, language);
            final QueryObjectModelFactory qomFactory = session.getWorkspace().getQueryManager().getQOMFactory();
            final ValueFactory valueFactory = session.getValueFactory();
            final Selector selector = qomFactory.selector(unescapedNodetype, SELECTOR_NAME);

            // language constraint: either jcr:language doesn't exist or jcr:language is current language
            Constraint constraint = qomFactory.or(
                    qomFactory.not(qomFactory.propertyExistence(SELECTOR_NAME, Constants.JCR_LANGUAGE)),
                    stringComparisonConstraint(qomFactory.propertyValue(SELECTOR_NAME, Constants.JCR_LANGUAGE), language, qomFactory, valueFactory)
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
            if (Utils.exists(orderBy)) {
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
            final Filter filter = Utils.getFilter(context);
            while (nodes.hasNext()) {
                final Node resultNode = nodes.nextNode();
                if (filter.acceptChild(resultNode)) {
                    JSONNode node = getFactory().createNode(resultNode, filter, depth);
                    result.add(node);
                }
            }

            return Response.ok(result).build();
        } catch (Exception e) {
            throw new APIException(e);
        } finally {
            closeSession(session);
        }
    }

    private Comparison stringComparisonConstraint(DynamicOperand operand, String valueOperandShouldBe, QueryObjectModelFactory qomFactory, ValueFactory valueFactory) throws RepositoryException {
        return qomFactory.comparison(operand, QueryObjectModelFactory.JCR_OPERATOR_EQUAL_TO, qomFactory.literal(valueFactory.createValue(valueOperandShouldBe,
                PropertyType.STRING)));
    }
}
