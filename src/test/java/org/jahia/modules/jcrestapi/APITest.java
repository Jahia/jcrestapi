/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
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
import org.apache.commons.io.FileUtils;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.jackrabbit.core.config.ConfigurationException;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.test.JerseyTest;
import org.jahia.modules.jcrestapi.api.PreparedQuery;
import org.jahia.modules.json.Names;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.securityfilter.PermissionService;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
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
import static org.jahia.modules.jcrestapi.APIApplication.SYS_PROP_DEPRECATION_FILTER_DISABLED;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        System.setProperty(SYS_PROP_DEPRECATION_FILTER_DISABLED, "true");
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
        System.clearProperty(SYS_PROP_DEPRECATION_FILTER_DISABLED);
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
        SpringBeansAccess.getInstance().setPermissionService(null);
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
        new SettingsBean(null, new Properties(), null) {
            @Override
            public int getMaxNameSize() {
                return 32;
            }
        };

        final String nodeType = "nt:address";
        final String generatedNodeName = JCRContentUtils.generateNodeName(nodeType, 32);
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

    @Test
    public void deleteShouldBeRefusedOnANodeTheScopeDoesNotAllow() throws Exception {

        final String name = "singleScoped";
        createNode("nt:address", name);

        denyOnlyTheDeleteOperation();

        given().when()
                .delete(getURLByPath("children/" + name))
                .then()
                .assertThat()
                .statusCode(SC_NOT_FOUND);
        assertTrue("the node should still be there once the operation is refused", rootHasChild(name));

        // the same request goes through once the scope allows the operation again
        SpringBeansAccess.getInstance().setPermissionService(null);
        given().when()
                .delete(getURLByPath("children/" + name))
                .then()
                .assertThat()
                .statusCode(SC_NO_CONTENT);
        assertFalse("the node should be gone once the operation is allowed", rootHasChild(name));
    }

    @Test
    public void batchDeleteShouldBeRefusedOnANodeTheScopeDoesNotAllow() throws Exception {

        final String name = "batchScoped";
        createNode("nt:address", name);

        denyOnlyTheDeleteOperation();

        given().body("[\"" + name + "\"]")
                .contentType(ContentType.JSON)
                .redirects().follow(false)
                .when()
                .delete(getURLByPath("children/"))
                .then()
                .assertThat()
                .statusCode(SC_NOT_FOUND);
        assertTrue("the node should still be there once the operation is refused", rootHasChild(name));

        // the same request goes through once the scope allows the operation again
        SpringBeansAccess.getInstance().setPermissionService(null);
        given().body("[\"" + name + "\"]")
                .contentType(ContentType.JSON)
                .redirects().follow(false)
                .when()
                .delete(getURLByPath("children/"))
                .then()
                .assertThat()
                .statusCode(SC_SEE_OTHER);
        assertFalse("the node should be gone once the operation is allowed", rootHasChild(name));
    }

    @Test
    public void batchDeleteOfTheNodeItselfShouldBeRefusedOnANodeTheScopeDoesNotAllow() throws Exception {

        // a batch payload sent on the node itself, i.e. with no sub-element type, deletes that very node
        final String name = "batchScopedItself";
        final String id = createNode("nt:address", name);

        denyOnlyTheDeleteOperation();

        // the refusal is read without following the redirect: the node's own URL answers 404 once the node is gone, so
        // a followed redirect would report the refused status on a request that in fact deleted the node
        given().body("[\"" + name + "\"]")
                .contentType(ContentType.JSON)
                .redirects().follow(false)
                .when()
                .delete(generateURL(getURIById(id)))
                .then()
                .assertThat()
                .statusCode(SC_NOT_FOUND);
        assertTrue("the node should still be there once the operation is refused", rootHasChild(name));

        // the same request goes through once the scope allows the operation again
        SpringBeansAccess.getInstance().setPermissionService(null);
        given().body("[\"" + name + "\"]")
                .contentType(ContentType.JSON)
                .redirects().follow(false)
                .when()
                .delete(generateURL(getURIById(id)))
                .then()
                .assertThat()
                .statusCode(SC_SEE_OTHER);
        assertFalse("the node should be gone once the operation is allowed", rootHasChild(name));
    }


    /**
     * A name reaches {@code Node.getNode(String)}, which resolves a relative path, so a name can reach a node
     * that is not below the one the request targets. What holds is that the scope is checked on the node the
     * request removes. Case contributed by baptistegrimaud in review.
     */
    @Test
    public void batchDeleteShouldCheckTheScopeOfANodeNamedThroughARelativePath() throws Exception {

        createNode("nt:address", "escapeSource");
        createNode("nt:address", "escapeVictim");
        denyTheDeleteOperationOnlyOn("/escapeVictim");

        given().body("[\"../escapeVictim\"]")
                .contentType(ContentType.JSON)
                .redirects().follow(false)
                .when()
                .delete(getURLByPath("escapeSource/children/"))
                .then()
                .assertThat()
                .statusCode(SC_NOT_FOUND);
        assertTrue("the target should still be there once its own scope refuses the operation",
                rootHasChild("escapeVictim"));

        // the same request goes through once the scope allows the operation on the target itself
        SpringBeansAccess.getInstance().setPermissionService(null);
        given().body("[\"../escapeVictim\"]")
                .contentType(ContentType.JSON)
                .redirects().follow(false)
                .when()
                .delete(getURLByPath("escapeSource/children/"))
                .then()
                .assertThat()
                .statusCode(SC_SEE_OTHER);
        assertFalse("the target should be gone once its own scope allows the operation",
                rootHasChild("escapeVictim"));
    }

    /**
     * Removing a deeper node in one request is what a name carrying a path is for, so it keeps working and the
     * scope is checked on that deeper node.
     */
    @Test
    public void batchDeleteShouldRemoveADeeperNodeNamedThroughAPath() throws Exception {

        makeParentAndChild("deepParent", "deepChild");
        denyTheDeleteOperationOnlyOn("/deepParent/deepChild");

        given().body("[\"deepParent/deepChild\"]")
                .contentType(ContentType.JSON)
                .redirects().follow(false)
                .when()
                .delete(getURLByPath("children/"))
                .then()
                .assertThat()
                .statusCode(SC_NOT_FOUND);
        assertTrue("the deeper node should still be there once its own scope refuses the operation",
                hasGrandChild("deepParent", "deepChild"));

        // the same request goes through once the scope allows the operation on that node
        SpringBeansAccess.getInstance().setPermissionService(null);
        given().body("[\"deepParent/deepChild\"]")
                .contentType(ContentType.JSON)
                .redirects().follow(false)
                .when()
                .delete(getURLByPath("children/"))
                .then()
                .assertThat()
                .statusCode(SC_SEE_OTHER);
        assertFalse("the deeper node should be gone once its own scope allows the operation",
                hasGrandChild("deepParent", "deepChild"));
    }

    @Test
    public void singleDeleteShouldBeRefusedOnAChildTheScopeDoesNotAllow() throws Exception {

        // the scope allows the operation on the parent the request resolves to, and refuses it on the child
        makeParentAndChild("scopedParentA", "scopedChildA");
        denyTheDeleteOperationOnlyOn("/scopedParentA/scopedChildA");

        given().when()
                .delete(getURLByPath("scopedParentA/children/scopedChildA"))
                .then()
                .assertThat()
                .statusCode(SC_NOT_FOUND);
        assertTrue("the child should still be there once the operation is refused",
                hasGrandChild("scopedParentA", "scopedChildA"));

        // the same request goes through once the scope allows the operation on the child too
        SpringBeansAccess.getInstance().setPermissionService(null);
        given().when()
                .delete(getURLByPath("scopedParentA/children/scopedChildA"))
                .then()
                .assertThat()
                .statusCode(SC_NO_CONTENT);
        assertFalse("the child should be gone once the operation is allowed",
                hasGrandChild("scopedParentA", "scopedChildA"));
    }

    @Test
    public void batchDeleteShouldBeRefusedOnAChildTheScopeDoesNotAllow() throws Exception {

        makeParentAndChild("scopedParentB", "scopedChildB");
        denyTheDeleteOperationOnlyOn("/scopedParentB/scopedChildB");

        given().body("[\"scopedChildB\"]")
                .contentType(ContentType.JSON)
                .redirects().follow(false)
                .when()
                .delete(getURLByPath("scopedParentB/children/"))
                .then()
                .assertThat()
                .statusCode(SC_NOT_FOUND);
        assertTrue("the child should still be there once the operation is refused",
                hasGrandChild("scopedParentB", "scopedChildB"));

        // the same request goes through once the scope allows the operation on the child too
        SpringBeansAccess.getInstance().setPermissionService(null);
        given().body("[\"scopedChildB\"]")
                .contentType(ContentType.JSON)
                .redirects().follow(false)
                .when()
                .delete(getURLByPath("scopedParentB/children/"))
                .then()
                .assertThat()
                .statusCode(SC_SEE_OTHER);
        assertFalse("the child should be gone once the operation is allowed",
                hasGrandChild("scopedParentB", "scopedChildB"));
    }

    @Test
    public void renameShouldBeRefusedOnANodeTheScopeDoesNotAllow() throws Exception {

        final String parent = "renameScopedParent";
        final String name = "renameScoped";
        final String newName = "renameScopedDone";
        final String id = createChildNode(parent, name);

        denyOnlyTheMoveOperation();

        given().redirects().follow(false)
                .when()
                .post(generateURL(getURIById(id) + "/moveto/" + newName))
                .then()
                .assertThat()
                .statusCode(SC_NOT_FOUND);
        assertTrue("the node should still carry its name once the operation is refused", hasGrandChild(parent, name));
        assertFalse("the node should not carry its new name once the operation is refused", hasGrandChild(parent, newName));

        // the same request goes through once the scope allows the operation again
        SpringBeansAccess.getInstance().setPermissionService(null);
        given().redirects().follow(false)
                .when()
                .post(generateURL(getURIById(id) + "/moveto/" + newName))
                .then()
                .assertThat()
                .statusCode(SC_SEE_OTHER);
        assertFalse("the node should have left its name once the operation is allowed", hasGrandChild(parent, name));
        assertTrue("the node should carry its new name once the operation is allowed", hasGrandChild(parent, newName));
    }

    /**
     * The scope is checked on the node the request resolves to, so a scope that refuses the move on one node leaves
     * the same request on another node alone.
     */
    @Test
    public void renameShouldBeRefusedOnTheNodeTheRequestResolvesTo() throws Exception {

        final String parent = "renameResolvedParent";
        final String refusedName = "renameRefused";
        final String allowedName = "renameAllowed";
        final String refusedId = createChildNode(parent, refusedName);
        final String allowedId = createChildNode(parent, allowedName);

        denyTheMoveOperationOnlyOn("/" + parent + "/" + refusedName);

        given().redirects().follow(false)
                .when()
                .post(generateURL(getURIById(refusedId) + "/moveto/renameRefusedDone"))
                .then()
                .assertThat()
                .statusCode(SC_NOT_FOUND);
        assertTrue("the refused node should still carry its name", hasGrandChild(parent, refusedName));

        given().redirects().follow(false)
                .when()
                .post(generateURL(getURIById(allowedId) + "/moveto/renameAllowedDone"))
                .then()
                .assertThat()
                .statusCode(SC_SEE_OTHER);
        assertFalse("the allowed node should have left its name", hasGrandChild(parent, allowedName));
        assertTrue("the allowed node should carry its new name", hasGrandChild(parent, "renameAllowedDone"));
    }

    /**
     * The new name resolves as a relative path, so a name such as {@code ../sibling/name} lands the node under
     * another parent. The rename is answered by that parent's own scope. Containment is not enforced, which is
     * deliberate: moving a node under another parent is what such a name is for.
     */
    @Test
    public void renameShouldCheckTheScopeOfTheParentTheNodeMovesUnder() throws Exception {

        final String parent = "renameDestParent";
        final String name = "renameDestChild";
        final String other = "renameDestOther";
        final String id = createChildNode(parent, name);
        createNode("nt:unstructured", other);

        denyTheMoveOperationOnlyOn("/" + other);

        given().redirects().follow(false).urlEncodingEnabled(false)
                .when()
                .post(generateURL(getURIById(id) + "/moveto/..%2F" + other + "%2Fmoved"))
                .then()
                .assertThat()
                .statusCode(SC_NOT_FOUND);
        assertTrue("the node should still carry its name once the destination refuses the operation",
                hasGrandChild(parent, name));
        assertFalse("the node should not have landed under the destination", hasGrandChild(other, "moved"));

        // the same request goes through once the destination's own scope allows the operation
        SpringBeansAccess.getInstance().setPermissionService(null);
        given().redirects().follow(false).urlEncodingEnabled(false)
                .when()
                .post(generateURL(getURIById(id) + "/moveto/..%2F" + other + "%2Fmoved"))
                .then()
                .assertThat()
                .statusCode(SC_SEE_OTHER);
        assertFalse("the node should have left its parent", hasGrandChild(parent, name));
        assertTrue("the node should have landed under the destination", hasGrandChild(other, "moved"));
    }

    /**
     * A name may reach a parent deeper in the tree, and that deeper node is the one the scope answers for.
     */
    @Test
    public void renameShouldMoveANodeUnderADeeperParentNamedThroughAPath() throws Exception {

        final String parent = "renameDeepParent";
        final String name = "renameDeepChild";
        final String id = createChildNode(parent, name);
        createChildNode("renameDeepOther", "deep");

        denyTheMoveOperationOnlyOn("/renameDeepOther/deep");

        given().redirects().follow(false).urlEncodingEnabled(false)
                .when()
                .post(generateURL(getURIById(id) + "/moveto/..%2FrenameDeepOther%2Fdeep%2Fmoved"))
                .then()
                .assertThat()
                .statusCode(SC_NOT_FOUND);
        assertTrue("the node should still carry its name", hasGrandChild(parent, name));

        SpringBeansAccess.getInstance().setPermissionService(null);
        given().redirects().follow(false).urlEncodingEnabled(false)
                .when()
                .post(generateURL(getURIById(id) + "/moveto/..%2FrenameDeepOther%2Fdeep%2Fmoved"))
                .then()
                .assertThat()
                .statusCode(SC_SEE_OTHER);
        session.refresh(false);
        assertTrue("the node should have landed under the deeper parent",
                session.getRootNode().getNode("renameDeepOther").getNode("deep").hasNode("moved"));
    }

    /**
     * A name of {@code .} carries no separator and still lands the node beside its own parent, so the node it
     * lands under is the one above. The scope answers for that node too.
     */
    @Test
    public void renameShouldCheckTheScopeWhenANameLandsTheNodeBesideItsOwnParent() throws Exception {

        final String parent = "renameDotParent";
        final String name = "renameDotChild";
        final String id = createChildNode(parent, name);

        denyTheMoveOperationOnlyOn("/");

        given().redirects().follow(false).urlEncodingEnabled(false)
                .when()
                .post(generateURL(getURIById(id) + "/moveto/."))
                .then()
                .assertThat()
                .statusCode(SC_NOT_FOUND);
        assertTrue("the node should still carry its name once the root refuses the operation",
                hasGrandChild(parent, name));
    }

    /**
     * A rename that keeps the node under its own parent reaches no node the request has not already answered for, so
     * the parent's own scope is not asked. Were it asked, a scope that covers a node without covering its container
     * would stop renaming that node, and the shipped {@code access_category} scope has exactly that shape.
     */
    @Test
    public void renameShouldNotAnswerForTheParentWhenTheNodeKeepsIt() throws Exception {

        final String parent = "renameKeepParent";
        final String name = "renameKeepChild";
        final String id = createChildNode(parent, name);
        final String outsideId = createChildNode("renameKeepOther", "renameKeepOutsider");

        denyTheMoveOperationOnlyOn("/" + parent);

        given().redirects().follow(false).urlEncodingEnabled(false)
                .when()
                .post(generateURL(getURIById(id) + "/moveto/renameKeepDone"))
                .then()
                .assertThat()
                .statusCode(SC_SEE_OTHER);
        assertTrue("a plain rename should go through while only the parent's scope refuses the operation",
                hasGrandChild(parent, "renameKeepDone"));

        // the same scope does refuse a rename that lands a node under that parent, so the arm above is not vacuous
        given().redirects().follow(false).urlEncodingEnabled(false)
                .when()
                .post(generateURL(getURIById(outsideId) + "/moveto/..%2F" + parent + "%2Fmoved"))
                .then()
                .assertThat()
                .statusCode(SC_NOT_FOUND);
        assertFalse("a rename landing a node under that parent should be refused", hasGrandChild(parent, "moved"));
    }

    private void makeParentAndChild(String parent, String child) throws RepositoryException {
        session.refresh(false);
        session.getRootNode().addNode(parent, "nt:unstructured").addNode(child, "nt:unstructured");
        session.save();
    }

    private boolean hasGrandChild(String parent, String child) throws RepositoryException {
        session.refresh(false);
        return session.getRootNode().hasNode(parent) && session.getRootNode().getNode(parent).hasNode(child);
    }

    /**
     * Installs a permission service that allows the delete operation everywhere but on one node path.
     */
    private void denyTheDeleteOperationOnlyOn(final String deniedPath) throws RepositoryException {

        final PermissionService permissionService = mock(PermissionService.class);
        when(permissionService.hasPermission(anyString(), any(Node.class))).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                final String api = (String) invocation.getArguments()[0];
                final Node node = (Node) invocation.getArguments()[1];
                return !(("jcrestapi." + API.DELETE).equals(api) && deniedPath.equals(node.getPath()));
            }
        });
        SpringBeansAccess.getInstance().setPermissionService(permissionService);
    }

    /**
     * Installs a permission service that allows every operation but the delete one, so that a refused delete cannot be
     * confused with a node the test can no longer read.
     */
    private void denyOnlyTheDeleteOperation() throws RepositoryException {

        final PermissionService permissionService = mock(PermissionService.class);
        when(permissionService.hasPermission(anyString(), any(Node.class))).thenReturn(true);
        when(permissionService.hasPermission(eq("jcrestapi." + API.DELETE), any(Node.class))).thenReturn(false);
        SpringBeansAccess.getInstance().setPermissionService(permissionService);
    }

    /**
     * Creates a node under the given parent, creating that parent first, and returns the identifier of the child.
     */
    private String createChildNode(String parent, String name) {
        createNode("nt:unstructured", parent);
        return given().body("{\"type\": \"nt:unstructured\"}")
                .contentType(ContentType.JSON)
                .when()
                .post(getURLByPath(parent + "/children/" + name))
                .then()
                .assertThat()
                .body("name", equalTo(name), "id", notNullValue())
                .extract().path("id");
    }

    /**
     * Installs a permission service that allows every operation but the move one, so that a refused rename cannot be
     * confused with a node the test can no longer read.
     */
    private void denyOnlyTheMoveOperation() throws RepositoryException {

        final PermissionService permissionService = mock(PermissionService.class);
        when(permissionService.hasPermission(anyString(), any(Node.class))).thenReturn(true);
        when(permissionService.hasPermission(eq("jcrestapi." + API.MOVE), any(Node.class))).thenReturn(false);
        SpringBeansAccess.getInstance().setPermissionService(permissionService);
    }

    /**
     * Installs a permission service that allows the move operation everywhere but on one node path.
     */
    private void denyTheMoveOperationOnlyOn(final String deniedPath) throws RepositoryException {

        final PermissionService permissionService = mock(PermissionService.class);
        when(permissionService.hasPermission(anyString(), any(Node.class))).thenAnswer(new Answer<Boolean>() {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                final String api = (String) invocation.getArguments()[0];
                final Node node = (Node) invocation.getArguments()[1];
                return !(("jcrestapi." + API.MOVE).equals(api) && deniedPath.equals(node.getPath()));
            }
        });
        SpringBeansAccess.getInstance().setPermissionService(permissionService);
    }

    /**
     * Reads the repository back through the test's own session rather than through the API, so that the result does not
     * depend on what the API is allowed to return.
     */
    private boolean rootHasChild(String name) throws RepositoryException {
        session.refresh(false);
        return session.getRootNode().hasNode(name);
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

    private String createNode(String nodeType, String name) {
        return given().body("{\"type\": \"" + nodeType + "\"}")
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
                ).extract().path("id");
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
