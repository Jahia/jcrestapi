/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
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

import com.jayway.restassured.http.ContentType;
import mockit.Mock;
import mockit.MockUp;
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.test.JerseyTest;
import org.jahia.modules.jcrestapi.api.PreparedQuery;
import org.jahia.modules.json.Names;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.settings.SettingsBean;
import org.junit.*;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static com.jayway.restassured.RestAssured.expect;
import static com.jayway.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;

/**
 * @author Christophe Laprun
 */
public class APITest extends JerseyTest {

    private static final String API_DEFAULT_EN = API.API_PATH + "/default/en/";
    private static final String API_DEFAULT_EN_BY_PATH = API_DEFAULT_EN + Paths.MAPPING + "/";
    private static final String API_DEFAULT_EN_NODES = API_DEFAULT_EN + Nodes.MAPPING + "/";
    private static TransientRepository repository;
    private static String repositoryLocation;
    private Session session;

    @BeforeClass
    public static void beforeAll() throws IOException, ConfigurationException {

        final Path repositoryPath = Files.createTempDirectory("jcrestapi-test-dir_");
        final InputStream configStream = APITest.class.getResourceAsStream("/repository.xml");

        final Path absolutePath = repositoryPath.toAbsolutePath();
        repositoryLocation = absolutePath.toString();
        final RepositoryConfig config = RepositoryConfig.create(configStream, repositoryLocation);
        repository = new NoLoggingTransientRepository(config);

        Runtime.getRuntime().addShutdownHook(new Thread("Repository Cleanup") {

            @Override
            public void run() {
                destroyRepository();
            }
        });
    }

    @AfterClass
    public static void destroyRepository() {
        if (repository != null) {
            repository.shutdown();
            repository = null;
        }
        try {
            FileUtils.deleteDirectory(new File(repositoryLocation));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    @Override
    public void setUp() throws Exception {

        super.setUp();

        PreparedQuery preparedQuery = new PreparedQuery();
        preparedQuery.setName("myQuery");
        preparedQuery.setSource("select * from [nt:nodeType] where [jcr:nodeTypeName] like ?");
        PreparedQueriesRegistry.getInstance().addQuery(preparedQuery);
        PreparedQuery preparedQuery2 = new PreparedQuery();
        preparedQuery2.setName("myQueryNamedParameters");
        preparedQuery2.setSource("select * from [nt:nodeType] where [jcr:nodeTypeName] like :nodeTypeName");
        PreparedQueriesRegistry.getInstance().addQuery(preparedQuery2);

//        // fake settings bean
//        final SettingsBean settingsBean = mock(SettingsBean.class);
//        Mockito.when(settingsBean.getMaxNameSize()).thenReturn(32);
//
//        // DANGER: must be careful with PowerMockito as it appears to replace ALL the static methods
//        // so you might get default return values for methods you don't expect
////        PowerMockito.mockStatic(SettingsBean.class);
////        PowerMockito.when(SettingsBean.getInstance()).thenReturn(settingsBean);

        session = repository.login();
    }

    @After
    public void afterEach() {
        session.logout();
    }

    @Override
    protected Application configure() {
        return new APIApplication(TestRepositoryFactory.class);
    }

    @Test
    public void testGetVersion() throws Exception {

        Properties props = new Properties();
        props.load(API.class.getClassLoader().getResourceAsStream(API.JCRESTAPI_PROPERTIES));

        given().accept(ContentType.TEXT)
                .when()
                .get(generateURL(API.API_PATH + "/version"))
                .then()
                .assertThat()
                .statusCode(SC_OK)
                .contentType(MediaType.TEXT_PLAIN)
                .body(equalTo("API version: " + API.API_VERSION + "\nModule version: " + API.getFullModuleVersion(props)));
    }

    @Test
    public void getVersionShouldProduceJSONIfAskedTo() throws Exception {

        Properties props = new Properties();
        props.load(API.class.getClassLoader().getResourceAsStream(API.JCRESTAPI_PROPERTIES));

        given().accept(ContentType.JSON)
                .when()
                .get(generateURL(API.API_PATH + "/version"))
                .then()
                .assertThat()
                .statusCode(SC_OK)
                .contentType(ContentType.JSON)
                .body(
                        "api", equalTo(API.API_VERSION),
                        "module", equalTo(API.getModuleVersion(props)),
                        "commit.id", equalTo(API.getCommitId(props)),
                        "commit.branch", equalTo(API.getCommitBranch(props))
                );
    }

    @Test
    public void checkAutomaticallyNamedChildren() throws Exception {
        // need write access to repository

        // using jmockit to provide a SettingsBean instance that returns a sane max name size without having to configure the whole bean
        new MockUp<SettingsBean>() {

            @Mock
            public SettingsBean getInstance() throws IOException {
                return new SettingsBean(null, new Properties(), null) {

                    @Override
                    public int getMaxNameSize() {
                        return 32;
                    }
                };
            }
        };

        final String nodeType = "nt:address";
        final String generatedNodeName = JCRContentUtils.generateNodeName(nodeType);
        final String first = given().body("{\"type\": \"" + nodeType + "\"}")
                .contentType(ContentType.JSON)
                .when()
                .post(getURLByPath("children"))
                .then()
                .assertThat()
                .statusCode(SC_CREATED)
                .contentType(Utils.MEDIA_TYPE_APPLICATION_HAL_PLUS_JSON)
                .body(
                        "name", equalTo(generatedNodeName),
                        "type", equalTo(nodeType),
                        "id", notNullValue()
                ).extract().path("id");

        given().body("{\"type\": \"" + nodeType + "\"}")
                .contentType(ContentType.JSON)
                .when()
                .post(getURLByPath("children"))
                .then()
                .assertThat()
                .statusCode(SC_CREATED)
                .contentType(Utils.MEDIA_TYPE_APPLICATION_HAL_PLUS_JSON)
                .body(
                        "name", allOf(startsWith(generatedNodeName), not(generatedNodeName)),
                        "type", equalTo(nodeType),
                        "id", not(first)
                );
    }

    @Test
    public void checkDelete() throws Exception {

        // create a node
        final String nodeType = "nt:address";
        final String name = "bar";
        createNode(nodeType, name);

        // then delete it
        final String urlByPath = getURLByPath("children/");
        given().when()
                .delete(urlByPath + name)
                .then()
                .assertThat()
                .statusCode(SC_NO_CONTENT);


        // verify that the child doesn't exist anymore
        expect().statusCode(SC_NOT_FOUND)
                .when().get(generateURL("/" + name));
    }

    @Test
    public void batchDeleteShouldWork() throws Exception {

        // create nodes
        final String nodeType = "nt:address";
        final String name = "bar";

        for (int i = 0; i < 5; i++) {
            createNode(nodeType, name + i);
        }

        // then batch delete some
        final String urlByPath = getURLByPath("children/");
        given().body("[\"bar0\", \"bar2\", \"bar4\"]")
                .contentType(ContentType.JSON)
                .when()
                .delete(urlByPath)/* // not sure why we get a 200 when the API really returns a 303 here :(
                .then()
                .assertThat()
                .statusCode(SC_SEE_OTHER)
                .header("Location", urlByPath)*/;

        // and finally check that they were properly removed
        expect().statusCode(SC_OK)
                .body(
                        "children", not(hasItems("bar0", "bar2", "bar4")),
                        createChildrenAssertions(nodeType, urlByPath, "bar1", "bar3")
                )
                .when()
                .get(urlByPath);
    }

    private Object[] createChildrenAssertions(String nodeType, String urlByPath, String... childNames) {
        if (childNames != null) {
            final Object[] result = new Object[childNames.length * 8];
            int i = 0;
            for (String childName : childNames) {
                result[i++] = "children." + childName + ".name";
                result[i++] = equalTo(childName);
                result[i++] = "children." + childName + ".type";
                result[i++] = equalTo(nodeType);
                result[i++] = "children." + childName + ".path";
                result[i++] = equalTo("/" + childName);
                result[i++] = "children." + childName + ".id";
                result[i++] = is(notNullValue());
            }
            return result;
        }
        return null;
    }

    private void createNode(String nodeType, String name) {
        given().body("{\"type\": \"" + nodeType + "\"}")
                .contentType(ContentType.JSON)
                .when()
                .post(getURLByPath("children/" + name))
                .then()
                .assertThat()
                .contentType(Utils.MEDIA_TYPE_APPLICATION_HAL_PLUS_JSON)
                .body(
                        "name", equalTo(name),
                        "type", equalTo(nodeType),
                        "id", notNullValue()
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

        final Node rootNode = session.getRootNode();
        final String rootId = rootNode.getIdentifier();
        final String rootTypeName = rootNode.getPrimaryNodeType().getName();

        expect().statusCode(SC_OK)
                .body(
                        "name", equalTo(""),
                        "type", equalTo("rep:root"),

                        // check that links are present
                        "_links.self.href", equalTo(getURIById(rootId)),
                        "_links.type.href", equalTo(getTypeURIByPath(rootTypeName)),
//                        todo: refactor to take into account filter
//                        "_links.children.href", equalTo(getChildURI(rootId, "children")),
//                        "_links.properties.href", equalTo(getChildURI(rootId, "properties")),
//                        "_links.mixins.href", equalTo(getChildURI(rootId, "mixins")),
//                        "_links.versions.href", equalTo(getChildURI(rootId, "versions")),

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
                .contentType(Utils.MEDIA_TYPE_APPLICATION_HAL_PLUS_JSON)
                .body(
                        "name", equalTo("jcr:system"),
                        "type", equalTo("rep:system")
                )
                .when().get(getURLByPath("jcr__system"));
    }

    @Test
    public void testOptions() {
        expect().statusCode(SC_OK)
                .when().options(getURLByPath(""));
    }

    @Test
    public void testQuery() {

        // create some children
        // create nodes
        final String nodeType = "nt:address";
        final String name = "bar";

        final int nodeNumber = 5;
        for (int i = 0; i < nodeNumber; i++) {
            createNode(nodeType, name + i);
        }

        // query is disabled by default
        given()
                .contentType("application/json")
                .body("{\"query\": \"SELECT * FROM [nt:base]\"}")
                .expect()
                .statusCode(SC_NOT_FOUND)
                .when()
                .post(generateURL(API_DEFAULT_EN + "query"));

        // activate query endpoint and retry
        API.setQueryDisabled(false);

        given()
                .contentType("application/json")
                .body("{\"query\": \"SELECT * FROM [" + nodeType + "] as node order by name(node)\"}")
                .queryParam("noLinks", "true")
                .expect()
                .statusCode(SC_OK)
                .contentType(Utils.MEDIA_TYPE_APPLICATION_HAL_PLUS_JSON)
                .body(".", hasSize(nodeNumber))
                .body("[0].path", equalTo("/bar0"))
                .body("[1].path", equalTo("/bar1"))
                .body("[2].path", equalTo("/bar2"))
                .body("[3].path", equalTo("/bar3"))
                .body("[4].path", equalTo("/bar4"))
                .when()
                .post(generateURL(API_DEFAULT_EN + "query"));

        final int limit = 3;
        given()
                .contentType("application/json")
                .body("{\"query\": \"SELECT * FROM [" + nodeType + "]\"," +
                        "\"limit\": " + limit + ",\"offset\" : 1}")
                .expect()
                .statusCode(SC_OK)
                .contentType(Utils.MEDIA_TYPE_APPLICATION_HAL_PLUS_JSON)
                .body(".", hasSize(limit))
                .body("[0].path", equalTo("/bar1"))
                .body("[1].path", equalTo("/bar2"))
                .body("[2].path", equalTo("/bar3"))
                .when()
                .post(generateURL(API_DEFAULT_EN + "query"));

        // now re-deactivate query, we should still be able to perform prepared queries
        API.setQueryDisabled(true);

        given()
                .contentType("application/json")
                .body("{\"queryName\": \"myQuery\"," +
                        "\"parameters\": [ \"nt:add%\" ]}")
                .expect()
                .statusCode(SC_OK)
                .contentType(Utils.MEDIA_TYPE_APPLICATION_HAL_PLUS_JSON)
                .body(".", hasSize(1))
                .body("[0].path", equalTo("/jcr:system/jcr:nodeTypes/nt:address"))
                .when()
                .post(generateURL(API_DEFAULT_EN + "query"));

        given()
                .contentType("application/json")
                .body("{\"queryName\": \"myQueryNamedParameters\"," +
                        "\"namedParameters\": { \"nodeTypeName\": \"nt:add%\" }}")
                .expect()
                .statusCode(SC_OK)
                .contentType(Utils.MEDIA_TYPE_APPLICATION_HAL_PLUS_JSON)
                .body(".", hasSize(1))
                .body("[0].path", equalTo("/jcr:system/jcr:nodeTypes/nt:address"))
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