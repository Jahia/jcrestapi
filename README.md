# RESTful JCR Access

## Overview

### REST, HATEOAS and HTTP

REST stands for REpresentational State Transfer. It is an architectural style defined by R. Fielding that underlies the modern web. On the web,
anything of interest can become a resource and be identified using a Universal Resource Identifier (URI). Resources are interacted with using representations that are passed
between clients and servers.

Hypermedia As The Engine Of Application State (HATEOAS) is a concept that states that all the information needed to interact with a resource should be contained in its
representation. Consider a web site. You first access it by typing its URL (a special kind of URI) in your browser's location bar. The server sends you a representation (HTML
page) of the web site's home page (the entry point resource). What you can do with that web page is contained in its representation: links, forms, etc. Of course,
you could try to figure out what other resources might exist on that server by manually crafting URIs but it's much easier to follow the information contained in the HTML page.
manipulated via representations that are passed between clients and servers. This is the essence of the HATEOAS concept.

HyperText Transfer Protocol (HTTP) is the protocol on which the web is build. It defines a uniform interface that both clients and servers agree to and with which they can
manipulate resources. In particular, the protocol defines methods (or verbs) corresponding to operations that can be done on resources. The main methods are:

- GET: retrieve the identified resource
- PUT: add / update the identified resource
- POST: should only be used for complex operations or to create resources from a factory resource (both if needed)
- DELETE: delete the identified resource

For a good (and not overly complex) overview of REST, please see (Jos Dirksen's REST: From GET to HATEOAS presentation)[http://www.slideshare
.net/josdirksen/rest-from-get-to-hateoas].

### Goals

The goals of this project are as follows:

- Provide a simple Create-Read-Update-Delete (CRUD) RESTful API to JCR content
- Leverage HATEOAS by providing all the required information to interact with a resource in its representation
- Support only JSON representations
- Optimize the API for Javascript client applications
- Provide full JCR access from the API with the associated benefits and risks

### Special provision for PUT and POST methods

PUT and POST methods theoretically exchange full resource representations. However, nodes and properties representations can be quite complex with a potentially deep graph-like
structure. Moreover, much of the exposed data is actually read-only (node type information, links...) or is or can be derived from the node type information. It would therefore
be inefficient to require clients to pass all that information to the API during creation or update of resources. We adopt the convention that only the information that is
either required or is being changed as a result of the operation is passed along to the API. This results in minimal effort on the client side and has the added benefit of
reducing the amount of network chatter. The semantics we follow is therefore close to the PATCH method semantics in spirit, if not in implementation.

---

## TODO:

- Improve cache control using ETag (for simple node GETs), in particular, can we use jcr:lastModified as an ETag and
using Response.cacheControl method instead of filter.
- Clarify usage of children vs. access via path and its consequences on NodeElementAccessor.
- <del>Design and implement versions access.</del> __(Done)__
- <del>Re-design URIs to provide easier access to workspace and language.</del> __(Done)__
- JS Client library?
- Improve cross-site support
- Improve authentication support, clarify which authentication options are supported
- <del>Define a versioning scheme</del> __(Done: use version number in the URIs)__
- <del>Should we use a vendor-specific content type?</del> __(Done: deemed too complex at the moment without much upside)__
- <del>JSON-P support</del> __(Done: no JSON-P support as after evaluation it's an inferior solution, focusing on CORS instead)__
- <del>Packaging</del> __(Done)__
- Documentation using apiary.io?
- <del>Support file uploads</del> __(Done)__
- Support easier creation of same-name siblings using POST and JSON data

---

## Resources identification

The natural match to map JCR data unto resources is to use JCR nodes as resources, identified either by their path or identifier,
which is made rather easy since JCR data is stored mostly in tree form.

A node also defines sub-resources:

- children for a given node are accessed using the `children` child resource
- properties for a given node are found under the `properties` child resource
- mixins for a given node are accessed using the `mixins` child resource
- versions for a given node are found under the `versions` child resource

Each of these sub-resources (which we also call _sub-element type_ as they identify a type of node sub-element) provides named access to their respective sub-elements as well.

---

## <a name="uri"/>URI design

- `:` character is encoded by `__` in property names since `:` is a reserved character for URIs
- indices of same name siblings are denoted using the `--` prefix

### Examples

| Node                        | Encoded URI                 |
| :-------------------------- | --------------------------- |
| `/foo/ns:bar/ns:child[2]`   | `/foo/ns__bar/ns__child--2` |
| `mix:title` mixin of `/foo` | `/foo/mixins/mix__title`    |
| `jcr:uuid` property of `/a` | `/a/properties/jcr__uuid`   |

---

## API entry points

The goal of this API is that you should be able to operate on its data using links provided within the returned representations.
However, you still need to be able to retrieve that first representation to work with in the first place.

### Base context

Since the API implementation is deployed as a module, it is available on your Jahia Digital Factory instance under the `/modules`
context with the `/api` specific context. Therefore, all URIs targeting the API will start with `/modules/api`. Keep this in mind
while looking at the examples below since we might not repeat the base context all the time.

We further qualify the base context by adding `/jcr/v1` to specify that this particular API deals with the JCR
domain and is currently in version 1. The scoping by domain allows us to potentially expand the API's reach to other aspects in the
future while we also make it clear which version (if/when several versions are needed) of that particular domain API is being used.

`<basecontext>` will henceforth refer to the `/modules/api/jcr/v1` base context below.

### API version

You can access the version of the API implementation performing a `GET` on the `<basecontext>/version` URI. This returns plain text information about both the version of the API
and of the currently running implementation. This can also serve as a quick check to see if the API is currently running or not.

### Workspace and language

You can access all the different workspaces and languages available in the Jahia Digital Factory JCR repository. However, you must
choose a combination of workspace _and_ language at any one time to work with JCR data. Which workspace and language to use are
specified in the URI path, using first, the escaped workspace name followed by the language code associated with the language you
wish to retrieve data in.

Therefore, all URIs targeting JCR data will be prefixed as follows: `<basecontext>/<workspace name>/<language code>/<rest of the URI>`

In the following sections, we detail the different types of URIs the API responds to. We will use `<placeholder>` or `{placeholder}`
indifferently to represent place holders in the different URIs. Each section will first present the URI template using the JAX-RS
`@Path` syntax for URIs which is quite self-explanatory for anyone with regular expression knowledge. We will then detail each part
of the URI template, specify the expected result, define which options if any are available and, finally, which HTTP operations can
be used on these URIs. Since we already talked about the workspace and language path elements, we won't address them in the following.

### Operating on nodes using their identifier

#### URI template
`/{workspace}/{language}/nodes/{id: [^/]*}{subElementType: (/children|mixins|properties|versions)?}{subElement: .*}`

#### URI elements

- `nodes`: path element marking access to JCR nodes from their identifier
- `{id: [^/]*}`: the identifier of the node we want to operate on, which is defined as all characters up to the next `/` character
- `{subElementType: (/children|mixins|properties|versions)?}`: an optional sub-element type to operate on the identified node's sub-resources
 as defined in the [URI Design](#uri) section
- `{subElement: .*}`: an optional sub-element escaped name to operate on a specific sub-resource of the identified node

If no `subElementType` path element is provided then no `subElement` path element can be provided either and the resource on which the API
will operate is the node identified by the specified `id` path element.

If a `subElementType` path element is provided but no `subElement` path element is provided, then the API will operate on the collection of
specified type of child resources for the node identified by the specified `id` path element.

If a `subElementType` path element is provided and a `subElement` path element is provided, then the API will operate on the child resource
identified by the `subElement` path element for the node identified by the specified `id` path element.

#### Examples

todo

#### Options

todo

#### Allowed HTTP operations

- `GET`: to retrieve the identified resource
- `PUT`: to create (if it doesn't already exist) or update the identified resource
- `DELETE`: to delete the identified resource

### Operating on nodes using their path

#### URI template
`/{workspace}/{language}/paths{path: /.*}`

#### URI elements

- `paths`: path element marking access to JCR nodes from their path
- `{path: /.*}`: the path of the resource to operate one

The `path` path element should contain the absolute path to a given JCR node with optional sub-element resolution if one of the child resource
names defined in the [URI Design](#uri) section is found. Note that once a sub-element is found, the node resolution will occur up to that
sub-element and the resolution of the sub-element will happen using the next path element, all others being discarded.

#### Examples

`<basecontext>/default/en/paths/users/root/profile` resolves to the `/users/root/profile` node in the `default` workspace using the `en` language.

`<basecontext>/live/fr/paths/sites/foo/properties/bar` resolves to the French (`fr` language) version of the `bar` property of the `/sites/foo` node
in the `live` workspace.

`<basecontext>/live/fr/paths/sites/foo/properties/bar/baz/foo` also resolves to the French version of the `bar` property of the `/sites/foo` node since
 only the next path element is considered when a sub-element type if found in one of the path elements of the considered URI.

#### Options

todo

#### Allowed HTTP operations

- `GET`: to retrieve the identified resource
- `POST`: to upload a file as a child node of the identified resource

### Retrieving nodes using their type

#### URI template
`{workspace}/{language}/types/{type}`

#### URI elements

- `types`: path element marking access to JCR nodes from their type
- `{type}`: the escaped name of the type of JCR nodes to retrieve

#### Examples

todo

#### Options

Options are specified using query parameters in the URI, further refining the request. Here is the list of available query parameters:

- `nameContains`: a possibly multi-valued String (by passing the query parameter several time in the URI) specifying which String(s) the retrieved
nodes must contain in their name. This is an `AND` constraint so further value of this parameter further limit the possible names.
- `orderBy`: a String specifying whether returned nodes should be ordered by ascending order (the default) or by descending order if the `desc`
value is passed.
- `limit`: an integer specifying how many nodes should be returned at most
- `offset`: an integer specifying how many nodes are skipped so that paging can be implemented
- `depth`: an integer specifying whether the returned nodes hierarchy is expanded to include sub-elements or not (default is `0` so no sub-elements
included)

#### Allowed HTTP operations

- `GET`: to retrieve the identified nodes

---

## Resources representation

This version of the API will use the [JSON](http://json.org) representation format as specified by the
[RFC 4627](http://www.ietf.org/rfc/rfc4627.txt) with the `application/json` media type,
with specific representations for node elements.

// todo: it might be worth it to describe the API using a JSON schema definition as per http://json-schema.org/

### <a name="linking"/>Linking to resources and keeping URIs opaque

It's important that clients don't rely on building URIs themselves as much as possible and thus,
we should strive to let clients treat them as *opaque* as possible. This does not mean that URIs need to be
unreadable. This, however, means that URIs for resources should be easily discoverable automatically by clients so
that they don't need to construct them. In order to do that, each resource that can be interacted with embeds a
`_links` child object. This child object contains links objects per the
[HAL](http://tools.ietf.org/html/draft-kelly-json-hal-06) specification.

This means that links are represented as objects containing at least an `href` property identifying the URI
associated with the link.

Per the HAL recommendations, we define a `self` reference identifying the URI to use to interact with this
specific element. `self` is a relative URI so we also provide an `absolute` link providing the absolute URI
for the given resource. If a resource has a type that we can identify then another `type` link will also be
available so that clients can find out more about the resource's metadata. If a resource can be accessed
via its path (in JCR parlance), then a `path` link pointing to the URI allowing access to the resource by
its path. When appropriate, another `parent` link will also be available, pointing to the parent node of
the resource. Specific objects might add more link types when appropriate.

To sum up, the `_links` section will look similarly to the following example:

    "_links" : {
        "self" : { "href" : "<relative URI (to the API base URI) identifying the associated resource>" },
        "absolute" : { "href" : "<absolute URI identifying the associated resource>" },
        "type" : { "href" : "<URI identifying the resource associated with the resource type>" }
        ... other links as appropriate
    }

// todo: examine whether it's worth using the JSON Hyper Schema specification for links: http://json-schema.org/latest/json-schema-hypermedia.html


### Node representation

A node is composed of several elements that need to be represented as efficiently and usefully as possible so that
both humans and automated systems can make sense of the information conveyed by the representation. We've identified
the following information to represent nodes:

- The unescaped node's name, represented by the `name` field, as detailed in the [Names section](#names).
- The name of the node's type, represented by the `type` field.
- The node's properties, which are gathered in a `properties` object, as detailed in the [Properties section](#properties).
- The node's children collection, which are gathered in a `children` object, as detailed in the [Children section]
(#children).
- The node's attached mixins, which information is gathered in a `mixins` object, as detailed in the [Mixins section]
(#mixins).
- The node's versions if appropriate, which are gathered in a `versions` object,
as detailed in the [Versions section](#versions).
- Links to useful resources associated with the node, gathered in `_links` object. By default,
only one level of depth in the hierarchy is retrieved for a given node. This means that reference
properties are not resolved to objects but left as strings. This also means that the API makes extensive use of links
between resources to discover and interact with associated resources. This is detailed in the [Linking section]
(#linking).

The node representation adds URIs identifying each sub-element to the `_links` section,
each named with the name of the sub-element it is associated with. This way, a `properties` object is added to the
`_links` section pointing to the `properties` resource, a `mixins` object points to the `mixins` resource, etc.

A node's collection resources allow users to query the particular kind of resource it holds or add new resource to
the set of existing ones. Collections should also considered as ordered despite not being modelled using JSON arrays.
We made this particular design decision because we felt being able to access a child resource via its name was more
important than explicitly modelling ordering.

### <a name="names"/>Names, escaped and unescaped

Since JCR property and node names are usually namespaced and the `:` character used as a namespace delimiter in the
prefixed form is a reserved JSON character, we use escaped (according to the rules outlined in the [URI design
section](#uri)) names to identify both properties and nodes in collections.
This way, client code can refer to properties and nodes without having to first escape them. However,
it can still be useful to know the original item name. We therefore provide an extra value in the item content named
quite appropriately `name` which contains the original, unescaped name of the item.

### Node representation structure

    "name" : <the node's unescaped name>,
    "type" : <the node's node type name>,
    "properties" : <properties representation>,
    "mixins" : <mixins representation>,
    "children" : <children representation>,
    "versions" : <versions representation>,
    "_links" : {
        "self" : { "href" : "<URI identifying the resource associated with this node>" },
        "type" : { "href" : "<URI identifying the resource associated with this node's type>" },
        "properties" : { "href" : "<URI identifying the resource associated with this node's properties>" },
        "mixins" : { "href" : "<URI identifying the resource associated with this node's mixins>" },
        "children" : { "href" : "<URI identifying the resource associated with this node's children>" },
        "versions" : { "href" : "<URI identifying the resource associated with this node's versions>" }
    }

Note that it should be possible for an API client to only request a subset of the complete structure. For example,
a client might only be interested in properties for a given call and not care about the rest of the structure. This
should be handled using query parameters during the `GET` request on the node resource.

### <a name="properties"/>Properties representation

A node's properties are gathered within a `properties` object that has the following structure:

    // other node elements...
    "properties" : {
        <for each property>
            <escaped property name> : <property representation>,
        </for each property>
        "_links" : {
            "self" : { "href" : "<URI identifying the resource associated with the parent's node properties resource>" },
            "parent" : { "href" : "<URI identifying the resource associated with the parent node>" }
        }
    },
    // other node elements...

Each property is represented by an object with the following structure:

    "name" : <unescaped name>,
    "multiValued" : <boolean indicating whether the property is multi-valued or not>
    "reference": <boolean indicating whether the property value(s) point(s) to a node or not>
    "value" : <value>,
    "type" : <type>,
    "_links" : {
        "self" : { "href" : "<URI identifying the resource associated with the property>" },
        "type" : { "href" : "<URI identifying the resource associated with the property definition>" }
    }

`type` is the case-insensitive name of the JCR property type, and is one of: `STRING`, `BINARY`, `LONG`, `DOUBLE`,
`DATE`, `BOOLEAN`, `NAME`, `PATH`, `REFERENCE`, `WEAKREFERENCE`, `URI`, and `DECIMAL`.

`multiValued` specifies whether the property is multi-valued or not. Having this field allows for easier processing of
properties on the client side without having to examine the property's definition.

The `reference` field specifies whether or not the property is of type `PATH`, `REFERENCE` or `WEAKREFERENCE`; in
essence whether or not the property's value is actually a pointer to a node. While this is merely a convenience since
that information can be inferred from the `type` field, this makes client processing easier.

If a property is a reference (its `reference` field is set to `true`), an additional `target` link is added to the `_links`
subsection, providing the URI identifying the resource identified by the path or reference value of the property.

#### Examples

An example of the `jcr:uuid` property of a `/sites/mySite` node. `jcr:uuid` is defined by the JCR specification as
being defined by the `mix:referenceable` mixin:

    "name" : "jcr:uuid",
    "value" : "039cdef3-289a-4fee-b80e-54da0ad35195",
    "type" : "string",
    "multiValued" : false,
    "reference" : false,
    "_links" : {
        "self" : { "href" : "http://api.example.org/sites/mySite/properties/jcr__uuid" },
        "type" : { "href" : "http://api.example.org/jcr__system/jcr__nodeTypes/mix__referenceable/jcr__propertyDefinition" }
    }

An example of the `jcr:mixinTypes` property on a `/sites/mySite` node.

    "name" : "jcr:mixinTypes",
    "multiValued" : true,
    "value" : ["jmix:accessControlled" , "jmix:robots"],
    "type" : "string",
    "reference" : false,
    "_links" : {
        "self" : { "href" : "http://api.example.org/sites/mySite/properties/jcr__mixinTypes" },
        "type" : { "href" : "http://api.example.org/jcr__system/jcr__nodeTypes/nt__base/jcr__propertyDefinition" }
    }

An example showing how indexed, same name properties URIs are represented, here the node type associated with the
property's definition is the second property defined on the `nt:base` node type:

    "name" : "jcr:primaryType",
    "value" : "jnt:virtualsite",
    "multiValued" : true,
    "type" : "string",
    "reference" : false,
    "_links" : {
        "self" : { "href" : "http://api.example.org/sites/mySite/properties/jcr__primaryType" },
        "type" : { "href" : "http://api.example.org/jcr__system/jcr__nodeTypes/nt__base/jcr__propertyDefinition--2" }
    }

An example showing how a `j:defaultSite` reference property pointing to a `/sites/mySite` node on a `/sites` node is
represented, demonstrating the `target` field in the `_links` section:

    "name" : "j:defaultSite",
    "value" :  "09100a94-0714-4fb6-98de-351ad63773b2",
    "multiValued" : false,
    "type" : "weakreference",
    "reference" : true,
    "_links" : {
        "self" : { "href" : "http://api.example.org/sites/properties/j__defaultSite" },
        "type" : { "href" : "http://api.example.org/jcr__system/jcr__nodeTypes/jnt__virtualsitesFolder/jcr__propertyDefinition--3" },
        "target" : { "href" : "http://api.example.org/sites/mySite" }
    }


### <a name="mixins"/>Mixins representation

A node's attached mixins information is gathered within a `mixins` object on the node's representation, as follows:

    // other node elements...
    "mixins" : {
        <for each mixin>
            <escaped mixin name> : <mixin representation>,
        </for each mixin>
        "_links" : {
            "self" : { "href" : "<URI identifying the resource associated with the parent's node mixins resource>" },
            "parent" : { "href" : "<URI identifying the resource associated with the parent node>" }
        }
    },
    // other node elements...

Here is the structure for a mixin representation:

    "name" : <the mixin's unescaped name>,
    "_links" : {
        "self" : { "href" : "<URI identifying the resource associated with the mixin in the context of the enclosing node>" },
        "type" : { "href" : "<URI identifying the resource associated with the mixin's node type>" }
    }

#### Examples

For a mixin named `jmix:robots` attached to a `/sites/mySite` node, we would use the following representation:

    "name" : "jmix:robots",
     "_links" : {
          "self" : { "href" : "http://api.example.org/sites/mySite/mixins/jmix__robots" },
          "type" : { "href" : "http://api.example.org/jcr__system/jcr__nodeTypes/jmix__robots" }
     }

Given the following mixin definition:

    [jmix:robots] mixin
         extends=jnt:virtualsite
        - robots (string, textarea) = 'User-agent: *'

To attach this mixin to an existing `/sites/mySite` node, a client would perform the following,
creating a new `jmix__robots` resource in the `mixins` collection resource, using a `PUT` request:

    PUT /sites/mySite/mixins/jmix__robots HTTP/1.1
    Host: api.example.org

    "properties" : {
        "robots" : {
            "value" : "User-agent: *"
        }
    }

To make things even simpler and since a property's value is the only thing that can be modified,
we could adopt the following convention to set a property's value, to be equivalent to the precedent request:

    PUT /sites/mySite/mixins/jmix__robots HTTP/1.1
    Host: api.example.org

    "properties" : {
        "robots" : "User-agent: *"
    }


### <a name="children"/>Children representation

Children of a given node are gathered within a `children` object, as follows:

    // other node elements...
    "children" : {
        <for each child>
            <escaped child name> : <child representation>,
        </for each child>
        "_links" : {
            "self" : { "href" : "<URI identifying the resource associated with the parent's node children resource>" },
            "parent" : { "href" : "<URI identifying the resource associated with the parent node>" }
        }
    },
    // other node elements...

Each child is represented by an object providing only minimal information about the child: its name,
its primary node type and its associated URIs (for both associated node, node type and parent node resources):

    "name" : <unescaped child name>,
    "type" : <nodetype name>,
    "_links" : {
        "self" : { "href" : "<URI identifying the resource associated with the child's node>" },
        "type" : { "href" : "<URI identifying the resource associated with the child's node type>" },
        "parent" : { "href" : "<URI identifying the resource associated with the parent node>" }
    }

#### Example

Below is the representation of a `tags` child element of a `/sites/mySite` node,
within the context of the enclosing's node `children` element:

    // ...
    "children" : {
        // ...

        "tags" : {
            "name" : "tags",
            "type" : "jnt:tagList",
            "_links" : {
                "self" : { "href" : "http://api.example.org/sites/mySite/tags" },
                "type" : { "href" : "http://api.example.org/jcr__system/jcr__nodeTypes/jnt__tagList" }
            }
        },

        // ...
        "_links" : {
            "self" : { "href" : "http://api.example.org/sites/mySite/children" }
        }
    }
    // ...

#### Special considerations for same-name siblings

It is possible for a node type to specify that instances of this node type allow multiple children with the same name.
They are then identified using a 1-based index representing the relative position of the child compared to other
instances of same-named children. They are created using the `children` resource of the parent node and are appended
at the end of the parent node's children collection. Their URIs and escaped names use the `--<index>` suffix
convention we discussed previously.

For example, assuming a `/foo` node allows for multiple `bar` children:

    # Adding a bar child
    PUT /foo/children/bar HTTP/1.1
    Host: api.example.org
    // bar content

    # Response
    HTTP/1.1 200 OK
    Content-Type: application/json

    "name" : "foo",
    // ...
    "children" : {
        "bar" : {
            "name" : "bar",
            "type" : "bar:nodeType",
            "_links" : {
                "self" : { "href" : "http://api.example.org/foo/bar" },
                // ...
            }
        },
        "_links" : {
            "self" : { "href" : "http://api.example.org/foo/children" }
        }
    }

    # Adding a bar child
    PUT /foo/children/bar HTTP/1.1
    Host: api.example.org
    // another bar child content

    # New response
    HTTP/1.1 200 OK
    Content-Type: application/json

    "name" : "foo",
    // ...
    "children" : {
        "bar" : {
            "name" : "bar",
            "type" : "bar:nodeType",
            "_links" : {
                "self" : { "href" : "http://api.example.org/foo/bar" },
                // ...
            }
        },
        "bar--2" : {
            "name" : "bar",
            "type" : "bar:nodeType",
            "_links" : {
                "self" : { "href" : "http://api.example.org/foo/bar--2" },
                // ...
            }
        },
        "_links" : {
            "self" : { "href" : "http://api.example.org/foo/children" }
        }
    }

// todo: how to support orderable children and in particular re-ordering operations?

### <a name="versions"/>Versions representation

A node's versions are gathered within a `versions` object as follows:

    // other node elements...
    "versions" : {
        <for each version>
            <escaped version name> : <version representation>,
        </for each version>
        "_links" : {
            "self" : { "href" : "<URI identifying the resource associated with the parent's node versions resource>" }
        }
    },
    // other node elements...

// todo
