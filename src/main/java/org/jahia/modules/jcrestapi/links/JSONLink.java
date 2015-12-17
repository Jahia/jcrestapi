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
 *     This program is free software; you can redistribute it and/or
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

import java.util.Arrays;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.jahia.modules.jcrestapi.Utils;

/**
 * @author Christophe Laprun
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public abstract class JSONLink {
    @XmlElement
    private String rel;

    private JSONLink(String rel) {
        this.rel = rel;
    }

    public static JSONLink createLink(String rel, Object link) {
        if (!Utils.exists(rel)) {
            throw new IllegalArgumentException("Must provide a valid relation. Was '" + rel + "'");
        }

        if (link instanceof String) {
            String linkAsString = (String) link;
            if (linkAsString.isEmpty()) {
                throw new IllegalArgumentException("Must provide a valid link. Was '" + link + "'");
            }
            return new SimpleJSONLink(rel, linkAsString);
        } else {
            if (link instanceof String[] && ((String[]) link).length > 0) {
                String[] links = (String[]) link;

                for (int i = 0; i < links.length; i++) {
                    String s = links[i];
                    if (!Utils.exists(s)) {
                        throw new IllegalArgumentException("All links in the link array must be non-null and non-empty. Invalid link at index " + i);
                    }
                }
                return new MultipleJSONLink(rel, links);
            }

            throw new IllegalArgumentException("Only String and String[] instances are currently supported as links. Was '" + link + "'");
        }

    }

    String getRel() {
        return rel;
    }

    public abstract Object getURI();

    public abstract String getURIAsString();

    public boolean isMultiple() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JSONLink jsonLink = (JSONLink) o;

        return rel.equals(jsonLink.rel) && getURI().equals(jsonLink.getURI());

    }

    @Override
    public int hashCode() {
        int result = rel.hashCode();
        result = 31 * result + getURI().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "JSONLink{" +
                "rel='" + rel + '\'' +
                ", uri=" + getURIAsString() +
                '}';
    }

    private static class SimpleJSONLink extends JSONLink {
        private final String link;

        private SimpleJSONLink(String rel, String link) {
            super(rel);
            this.link = link;
        }

        @Override
        @XmlElement(name = "href")
        public Object getURI() {
            return link;
        }

        @Override
        public String getURIAsString() {
            return link;
        }
    }

    private static class MultipleJSONLink extends JSONLink {
        private final String[] links;

        public MultipleJSONLink(String rel, String[] link) {
            super(rel);
            this.links = link;
        }

        @Override
        @XmlElement(name = "href")
        public Object getURI() {
            return links;
        }

        @Override
        public String getURIAsString() {
            return Arrays.toString(links);
        }

        @Override
        public boolean isMultiple() {
            return true;
        }
    }
}
