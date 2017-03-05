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

import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;

/**
 * @author Christophe Laprun
 */
@Provider
public class APIExceptionMapper implements ExceptionMapper<APIException> {
    public static final transient Logger LOGGER = org.slf4j.LoggerFactory.getLogger(API.class);

    private Response.ResponseBuilder toResponse(RepositoryException exception) {
        if (exception instanceof ItemNotFoundException || exception instanceof PathNotFoundException) {
            return Response.status(Response.Status.NOT_FOUND);
        }
        return defaultResponse(exception);
    }

    private Response.ResponseBuilder defaultResponse(Throwable exception) {
        return Response.serverError().entity(new APIException.JSONError(exception));
    }

    @Override
    public Response toResponse(APIException exception) {
        final Throwable cause = exception.getCause();

        LOGGER.debug("An error occurred in the RESTful API", cause);

        Response.ResponseBuilder builder;
        if (cause instanceof RepositoryException) {
            builder = toResponse((RepositoryException) cause);
        } else if (cause instanceof UnsupportedOperationException) {
            builder = Response.status(Response.Status.METHOD_NOT_ALLOWED);
        }
        else {
            builder = defaultResponse(cause);
        }

        final APIException.JSONError error = exception.getError();
        return error != null ? builder.entity(error).build() : builder.build();
    }
}
