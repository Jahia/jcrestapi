/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 * Copyright (C) 2002-2015 Jahia Solutions Group SA. All rights reserved.
 *
 * THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 * 1/GPL OR 2/JSEL
 *
 * 1/ GPL
 * ======================================================================================
 *
 * IF YOU DECIDE TO CHOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 * "This program is free software; you can redistribute it and/or
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
 * describing the FLOSS exception, also available here:
 * http://www.jahia.com/license"
 *
 * 2/ JSEL - Commercial and Supported Versions of the program
 * ======================================================================================
 *
 * IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 * Alternatively, commercial and supported versions of the program - also known as
 * Enterprise Distributions - must be used in accordance with the terms and conditions
 * contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 * If you are unsure which license is appropriate for your use,
 * please contact the sales department at sales@jahia.com.
 *
 *
 * ==========================================================================================
 * =                                   ABOUT JAHIA                                          =
 * ==========================================================================================
 *
 * Rooted in Open Source CMS, Jahia’s Digital Industrialization paradigm is about
 * streamlining Enterprise digital projects across channels to truly control
 * time-to-market and TCO, project after project.
 * Putting an end to “the Tunnel effect”, the Jahia Studio enables IT and
 * marketing teams to collaboratively and iteratively build cutting-edge
 * online business solutions.
 * These, in turn, are securely and easily deployed as modules and apps,
 * reusable across any digital projects, thanks to the Jahia Private App Store Software.
 * Each solution provided by Jahia stems from this overarching vision:
 * Digital Factory, Workspace Factory, Portal Factory and eCommerce Factory.
 * Founded in 2002 and headquartered in Geneva, Switzerland,
 * Jahia Solutions Group has its North American headquarters in Washington DC,
 * with offices in Chicago, Toronto and throughout Europe.
 * Jahia counts hundreds of global brands and governmental organizations
 * among its loyal customers, in more than 20 countries across the globe.
 *
 * For more information, please visit http://www.jahia.com
 */
package org.jahia.modules.jcrestapi;

import com.jayway.restassured.http.ContentType;
import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.test.JerseyTest;
import org.jahia.modules.json.Names;
import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import java.util.Properties;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.*;

/**
 * @author Christophe Laprun
 */
public class APITest extends JerseyTest {

    private static final String API_DEFAULT_EN = API.API_PATH + "/default/en/";
    private static final String API_DEFAULT_EN_BY_PATH = API_DEFAULT_EN + Paths.MAPPING + "/";
    private static final String API_DEFAULT_EN_NODES = API_DEFAULT_EN + Nodes.MAPPING + "/";

    @Override
    protected Application configure() {
        return new APIApplication(TestRepositoryFactory.class);
    }

    @Test
    public void testGetVersion() throws Exception {
        Properties props = new Properties();
        props.load(API.class.getClassLoader().getResourceAsStream(API.JCRESTAPI_PROPERTIES));

        expect().statusCode(SC_OK)
                .contentType(MediaType.TEXT_PLAIN)
                .body(equalTo("API version: " + API.API_VERSION + "\nModule version: " + API.getFullModuleVersion(props)))
                .when().get(generateURL(API.API_PATH + "/version"));
    }

    @Test
    public void getVersionShouldProduceJSONIfAskedTo() throws Exception {
        Properties props = new Properties();
        props.load(API.class.getClassLoader().getResourceAsStream(API.JCRESTAPI_PROPERTIES));

        given().
                accept(ContentType.JSON).
                when().
                get(generateURL(API.API_PATH + "/version")).
                then().
                assertThat().
                statusCode(SC_OK).
                contentType(ContentType.JSON).
                body(
                        "api", equalTo(API.API_VERSION),
                        "module", equalTo(API.getModuleVersion(props)),
                        "commit.id", equalTo(API.getCommitId(props)),
                        "commit.branch", equalTo(API.getCommitBranch(props))
                );
    }

    @Test
    public void testGetInexistingNode() {
        expect().statusCode(SC_NOT_FOUND)
                .when().get(generateURL("/foo"));

        expect().statusCode(SC_NOT_FOUND)
                .when().get(getURLByPath("foo"));
    }

    @Test
    public void testGetRoot() throws RepositoryException {
        final Session session = TestRepositoryFactory.repository.login();
        final Node rootNode = session.getRootNode();
        final String rootId = rootNode.getIdentifier();
        final String rootTypeName = rootNode.getPrimaryNodeType().getName();
        session.logout();

        expect().statusCode(SC_OK)
                .body(
                        "name", equalTo(""),
                        "type", equalTo("rep:root"),

                        // check that links are present
                        "_links.self.href", equalTo(getURIById(rootId)),
                        "_links.type.href", equalTo(getTypeURIByPath(rootTypeName)),
                        "_links.children.href", equalTo(getChildURI(rootId, "children")),
                        "_links.properties.href", equalTo(getChildURI(rootId, "properties")),
                        "_links.mixins.href", equalTo(getChildURI(rootId, "mixins")),
                        "_links.versions.href", equalTo(getChildURI(rootId, "versions")),

                        // check jcr:primaryType property
                        "properties.jcr__primaryType.name", equalTo("jcr:primaryType"),
                        "properties.jcr__primaryType.value", equalTo("rep:root"),
                        "properties.jcr__primaryType._links.self.href", equalTo(getChildURI(rootId, "properties/jcr__primaryType")),
                        "properties.jcr__primaryType._links.type.href", equalTo(getTypeURIByPath("nt__base/jcr__propertyDefinition--2")),
                        "properties.jcr__primaryType._links.path.href", equalTo(getURIByPath("properties/jcr__primaryType")),

                        // check jcr:mixinTypes property
                        "properties.jcr__mixinTypes.name", equalTo("jcr:mixinTypes"),
                        "properties.jcr__mixinTypes.value", hasItem("rep:AccessControllable"),
                        "properties.jcr__mixinTypes._links.self.href", equalTo(getChildURI(rootId, "properties/jcr__mixinTypes")),
                        "properties.jcr__mixinTypes._links.type.href", equalTo(getTypeURIByPath("nt__base/jcr__propertyDefinition")),

                        // check that children don't have children (only 1 level deep hierarchy)
                        "children.jcr__system.children", is(nullValue())
                )
                .when().get(getURLByPath(""));
    }

    /*@Test
    public void testThatWeCanAccessValuesAndTypesFromLinks() {
        // get root and its JSON representation
        final Response response = expect().statusCode(SC_OK).when().get(getURLByPath(""));
        final JsonPath rootJSON = response.body().jsonPath();

        // get the root primary type property and check that its name matches the one we got from root object
        final String primaryTypeSelf = rootJSON.getString("properties.jcr__primaryType._links.self.href");

        expect().body(
                "name", equalTo(rootJSON.get("properties.jcr__primaryType.name"))
        ).when().get(generateURL(primaryTypeSelf));

        // get the root primary type property definition and check that we're getting a property definition
        final String primaryTypeType = rootJSON.getString("properties.jcr__primaryType._links.type.href");
        expect().body(
                "type", equalTo("nt:propertyDefinition"),
                "properties.jcr__name.value", equalTo("jcr:primaryType")
        ).when().get(getURIByPath(primaryTypeType));
    }*/

    @Test
    public void testGetJCRSystem() {
        expect().statusCode(SC_OK)
                .contentType("application/hal+json")
                .body(
                        "name", equalTo("jcr:system"),
                        "type", equalTo("rep:system")
                )
                .when().get(getURLByPath("jcr__system"));
    }

    @Test
    public void testQuery() {
        given()
                .contentType("application/json")
                .body("{\"query\": \"SELECT * FROM [nt:base]\"}")
                .expect()
                .statusCode(SC_OK)
                .contentType("application/hal+json")
                .body(".", hasSize(188))
                .body("[0].path", equalTo("/"))
                .when()
                .post(generateURL(API_DEFAULT_EN + "query"));

        given()
                .contentType("application/json")
                .body("{\"query\": \"SELECT * FROM [nt:base]\"," +
                        "\"limit\": 10, " +
                        "\"offset\" : 1}")
                .expect()
                .statusCode(SC_OK)
                .contentType("application/hal+json")
                .body(".", hasSize(10))
                .body("[0].path", equalTo("/jcr:system"))
                .when()
                .post(generateURL(API_DEFAULT_EN + "query"));

    }

    private String generateURL(String path) {
        return target(path).getUri().toASCIIString();
    }

    private String getURLByPath(String path) {
        return generateURL(API_DEFAULT_EN_BY_PATH + path);
    }

    private String getURIById(String id) {
        return API_DEFAULT_EN_NODES + id;
    }

    private String getURIByPath(String path) {
        return API_DEFAULT_EN_BY_PATH + Names.escape(path);
    }

    private String getTypeURIByPath(String typeName) {
        return API_DEFAULT_EN_BY_PATH + "jcr__system/jcr__nodeTypes/" + Names.escape(typeName);
    }

    private String getChildURI(String rootId, String childName) {
        return getURIById(rootId) + "/" + childName;
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

    private static class TestRepositoryFactory implements Factory<Repository> {
        static final Repository repository = new NoLoggingTransientRepository();

        @Override
        public Repository provide() {
            return repository;
        }

        @Override
        public void dispose(Repository instance) {
            // nothing
        }
    }

}