/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2016 Jahia Solutions Group SA. All rights reserved.
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
package org.jahia.modules.jcrestapi.links;

import org.jahia.modules.jcrestapi.API;
import org.junit.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;

/**
 * @author Christophe Laprun
 */
public class JSONLinkTest {

    @Test(expected = IllegalArgumentException.class)
    public void constructingWithNullRelShouldFail() {
        JSONLink.createLink(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructingWithNullLinkShouldFail() {
        JSONLink.createLink(API.SELF, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructingWithEmptyRelShouldFail() {
        JSONLink.createLink("", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructingWithEmptyLinkShouldFail() {
        JSONLink.createLink("foo", "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructingWithEmptyLinkArrayShouldFail() {
        JSONLink.createLink("foo", new String[]{});
    }

    @Test
    public void constructingWithArrayShouldFailIfALinkIsEmptyOrNull() {
        try {
            JSONLink.createLink("foo", new String[]{""});
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            JSONLink.createLink("foo", new String[]{null});
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            JSONLink.createLink("foo", new String[]{"foo", null});
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            // expected
        }

        try {
            JSONLink.createLink("foo", new String[]{"foo", ""});
            failBecauseExceptionWasNotThrown(IllegalArgumentException.class);
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructingWithOtherThanStringOrStringArrayShouldFail() {
        JSONLink.createLink("foo", new Object());
    }

    @Test
    public void checkAccessorsOnSimpleLink() {
        final String rel = "rel";
        final String link = "link";
        final JSONLink jsonLink = JSONLink.createLink(rel, link);
        assertThat(jsonLink).isNotNull();
        assertThat(jsonLink.getRel()).isEqualTo(rel);
        assertThat(jsonLink.getURI()).isEqualTo(link);
        assertThat(jsonLink.getURIAsString()).isEqualTo(link);
        assertThat(jsonLink.isMultiple()).isFalse();
    }

    @Test
    public void checkAccessorsOnArrayLink() {
        final String rel = "rel";
        final String[] links = new String[]{"link1", "link2"};
        final JSONLink jsonLink = JSONLink.createLink(rel, links);
        assertThat(jsonLink).isNotNull();
        assertThat(jsonLink.getRel()).isEqualTo(rel);
        assertThat(jsonLink.getURI()).isEqualTo(links);
        assertThat(jsonLink.getURIAsString()).isEqualTo(Arrays.toString(links));
        assertThat(jsonLink.isMultiple()).isTrue();
    }
}
