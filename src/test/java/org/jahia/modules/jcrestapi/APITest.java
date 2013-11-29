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
import org.apache.jackrabbit.core.TransientRepository;
import org.jboss.resteasy.plugins.server.sun.http.HttpContextBuilder;
import org.jboss.resteasy.spi.ResteasyDeployment;
import org.jboss.resteasy.test.TestPortProvider;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.Properties;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.get;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
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
        final ResteasyDeployment deployment = contextBuilder.getDeployment();
        deployment.getActualResourceClasses().add(APIWithFixture.class);
        contextBuilder.bind(httpServer);

        // to make sure our ExceptionMapper is properly registered
        deployment.getProviderFactory().registerProvider(APIExceptionMapper.class);

        httpServer.start();
    }

    @AfterClass
    public static void stop() throws Exception {
        contextBuilder.cleanup();
        httpServer.stop(0);
    }

    @Test
    public void testGetVersion() throws Exception {
        Properties props = new Properties();
        props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("jcrestapi.properties"));

        expect().statusCode(SC_OK)
                .contentType("text/plain")
                .body(equalTo(props.getProperty("jcrestapi.version")))
                .when().get(getURL("version"));
    }

    @Test
    public void testGetInexistingNode() {
        expect().statusCode(SC_NOT_FOUND)
                .when().get(generateURL("/foo"));

        expect().statusCode(SC_NOT_FOUND)
                .when().get(getURL("foo"));
    }

    @Test
    public void testGetRoot() {

        System.out.println(get(getURL("")).asString());

        expect().statusCode(SC_OK)
                .contentType("application/hal+json")
                .body(
                        "name", equalTo(""),
                        "type", equalTo("rep:root"),
                        "properties.jcr__primaryType.name", equalTo("jcr:primaryType"),
                        "properties.jcr__primaryType.value", equalTo("rep:root"),
                        "properties.jcr__mixinTypes.name", equalTo("jcr:mixinTypes"),
                        "properties.jcr__mixinTypes.value", hasItem("rep:AccessControllable")
                )
                .when().get(getURL(""));
    }

    @Test
    public void testGetJCRSystem() {

        System.out.println(get(getURL("jcr__system")).asString());

        expect().statusCode(SC_OK)
                .contentType("application/json")
                .body(
                        "name", equalTo("jcr:system"),
                        "type", equalTo("rep:system")
                )
                .when().get(getURL("jcr__system"));
    }

    private String getURL(String path) {
        return generateURL("/api/" + path);
    }

    /*@Test
    public void testGetSite() {
        expect().statusCode(SC_OK)
                .contentType("application/json")
                .body(
                        "props.j__nodename.value", equalTo("site"),
                        "props.j__nodename.type", equalToIgnoringCase("string")
                )
                .when().get(generateURL("/sites/site"));
    }*/

    /*
    @Test
    public void createSite() {
        given().body("\"j__title\":\"mySite\"")
                .expect()
                .statusCode(SC_CREATED)
                .header(LOCATION, baseURL + "/sites/mySite")
                .body("j__nodename.value", equalTo("mySite"), "j__nodename.type", equalTo("string"),
                        "j__nodename.links.self", equalTo(baseURL + "/sites/mySite/props/j__nodename"))
                .when().put("/sites/mySite");
    }

    @Test
    public void attemptingToChangeAProtectedPropertyShouldFail(@ArquillianResource URL baseURL) {
        final String propURI = baseURL + "/sites/mySite/props/j__nodename";
        given().body("newSite")
                .expect()
                .statusCode(SC_METHOD_NOT_ALLOWED)
                .header(LOCATION, propURI)
                .header(ALLOW, "GET")
                .when().put(propURI);
    }*/

    /**
     * Instrumented API implementation so that we can get a simple way to set up the API with test fixtures since Spring
     * injection won't work directly due to the fact that the API bean is loaded in the RESTeasy context in the HTTP
     * server process.
     */
    public static class APIWithFixture extends API {
        public APIWithFixture() {
            setRepository(new TransientRepository());
        }
    }
}