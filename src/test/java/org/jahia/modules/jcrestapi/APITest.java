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

import com.sun.net.httpserver.HttpServer;
import org.jboss.resteasy.plugins.server.sun.http.HttpContextBuilder;
import org.jboss.resteasy.test.TestPortProvider;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.InetSocketAddress;
import java.util.Properties;

import static org.jboss.resteasy.test.TestPortProvider.generateURL;

/**
 * @author Christophe Laprun
 */
public class APITest {

    private static HttpServer httpServer;
    private static HttpContextBuilder contextBuilder;


    @BeforeClass
    public static void init() throws Exception {
        int port = TestPortProvider.getPort();

        httpServer = HttpServer.create(new InetSocketAddress("localhost", port), 10);
        contextBuilder = new HttpContextBuilder();
        contextBuilder.getDeployment().getActualResourceClasses().add(API.class);
        contextBuilder.bind(httpServer);
        httpServer.start();

    }

    @AfterClass
    public static void stop() throws Exception {
        contextBuilder.cleanup();
        httpServer.stop(0);
    }

    @Test
    public void testGetVersion() throws Exception {
        Client client = ClientBuilder.newClient();
        final WebTarget target = client.target(generateURL("/version"));
        final Response response = target.request("text/plain").get();
        String version = response.readEntity(String.class);

        Properties props = new Properties();
        props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("jcrestapi.properties"));

        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        Assert.assertEquals(props.getProperty("jcrestapi.version"), version);
        client.close();
    }
}