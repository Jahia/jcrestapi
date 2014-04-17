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
package org.jahia.modules.jcrestapi.accessors;

import org.jahia.modules.jcrestapi.API;
import org.jahia.modules.jcrestapi.NodeUtil;
import org.jahia.modules.jcrestapi.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.assertj.core.api.Assertions.assertThat;

import javax.jcr.RepositoryException;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * @author Christophe Laprun
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(API.class)
public abstract class ElementAccessorTest<C extends JSONSubElementContainer, T extends JSONLinkable, U extends JSONItem> {

    static final String WORKSPACE = "default";
    static final String LANGUAGE = "en";

    @Before
    public void setUp() {
        // fake session, at least to get access to a workspace name and language code for URIUtils
        PowerMockito.mockStatic(API.class);
        PowerMockito.when(API.getCurrentSession()).thenReturn(new API.SessionInfo(null, WORKSPACE, LANGUAGE));
    }

    @Test
    public void testPerformMinimalSingleReadNoSubElement() throws RepositoryException {
        final Response response = getAccessor().perform(NodeUtil.createMockNode(), (String) null, API.READ, null, null);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.OK);

        C container = getContainerFrom(response);
        final Map<String, JSONLink> links = container.getLinks();
        assertThat(links).containsKeys(API.ABSOLUTE, API.SELF, API.PARENT);
    }

    protected abstract C getContainerFrom(Response response);

    public abstract ElementAccessor<C, T, U> getAccessor();
}
