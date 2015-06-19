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

- `GET`: retrieve the identified resource
- `PUT`: add / update the identified resource
- `POST`: should only be used for complex operations or to create resources from a factory resource (both if needed)
- `DELETE`: delete the identified resource

For a good (and not overly complex) overview of REST, please see
[Jos Dirksen's REST: From GET to HATEOAS presentation](http://www.slideshare.net/josdirksen/rest-from-get-to-hateoas).

### API version history

- v1.0: initial release
- v1.1: 
    - `PUT` and `DELETE` methods are now supported when operating on nodes via their path
    - added `includeFullChildren`, `resolveReferences` and `noLinks` flags that can be passed as URI query parameters to control some aspects of the representations
    - added new `query` endpoint to perform `JCR-SQL2` queries on the repository and retrieve matching nodes, disabled by default for security reasons, following the `jahia.find
    .disabled` property.
    - order of children is now properly maintained
    - it is now possible to filter children to retrieve by providing a list of accepted child node types concatenated by commas
- v1.1.1:
    - added support for prepared queries to the query endpoint. This is now the preferred way to use the query endpoint since prepared queries will still be available
      even when the query endpoint is disabled since they are considered "safe" by the administrator who deployed them.
    - flags are now also supported on query endpoint
      
### Implementation version history

- v2.0.0: initial release
- v2.0.1: 
    - properly use `application/hal+json` content type on responses
    - improved links support
    - minor improvements
- v2.1.0:
    - support for v1.1 of the API
    - fixed an issue with Paths API where extra slashes in the URI could result in unexpected behavior
- v2.1.1:
    - support for disabling query and types endpoint based on `jahia.find.disabled` setting.

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
- Migrate String constants (link relations, sub-element types, etc.) to typesafe constants organized by type

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

## Basic API workflow

Using the API is then a matter of:

1. Identifying which resource to work with.
2. Deciding which HTTP method to use to operate on the identified resource.
3. Invoke the HTTP method on the resource passing it any necessary data.

Since client and server exchange representations to signify state changes, when you need to pass data to create or update a resource, the API server expects a
representation it can understand. In this particular implementation, representations that are expected as input data of operations __MUST__ have the exact same
structure as the representation you would retrieve performing a `GET` operation on that particular resource.

So, for example, if you're trying to add several properties to a given node, you could do it several different ways. Either you could `PUT` each property individually using each
 property URI as target URI and passing the JSON representation of a property for each invocation. You could also use the `properties` sub-resource for this node and invoke the
 `PUT` method with a body corresponding to the JSON representation of a `properties` resource. This second way would result in modifying several properties using a single call
 to the API.
 
__Note regarding asynchronous calls to the RESTful API:__ While the API implementation can properly handle concurrent requests at any given time, 
it is not designed to handle <strong>re-entrant</strong> requests. This will result in an exception. This could happen if you're calling the API from Javascript and you're 
calling back to the API in your success callback in an asynchronous call. There normally shouldn't be a need for such calls which is why we are not currently supporting this use
case. We might revisit this position if such need arises.

---

## Resources representation

This version of the API will use the [JSON](http://json.org) representation format as specified by the
[RFC 4627](http://www.ietf.org/rfc/rfc4627.txt) augmented by the [HAL](http://tools.ietf.org/html/draft-kelly-json-hal-06) specification as explained below. The media type for
our specific representations for node elements is therefore `application/hal+json`.

Note that representations elements are not ordered so you shouldn't depend on elements of a given representation being in a specific order. In particular,
if elements appear in a given order in examples in this document this doesn't mean that they will appear in the same order in representations you will retrieve from the API.

Note also that we focus only on salient parts in examples so the representations that are shown might actually be incomplete and missing some data that we didn't consider
relevant for that specific example. In particular, links sections are often elided in examples for brevity's sake.

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

We add a little bit of redundant information to make links easier to work with from a javascript client application: we add a `rel` property to each link that repeats the name
of the link from within it so that when iterating over the links collection, we can easily retrieve the name of the relation from an individual link object.

To sum up, the `_links` section will look similarly to the following example:

    "_links" : {
        "self" : {
            "rel" : "self",
            "href" : "<relative URI (to the API base URI) identifying the associated resource>"
        },
        "absolute" : {
            "rel" : "absolute",
            "href" : "<absolute URI identifying the associated resource>"
        },
        "type" : {
            "rel" : "type",
            "href" : "<URI identifying the resource associated with the resource type>"
        }
        ... other links as appropriate
    }

// todo: examine whether it's worth using the JSON Hyper Schema specification for links: http://json-schema.org/latest/json-schema-hypermedia.html

As of version 1.1 of the API, we've added the option not to output links if your client application has no need for them.
This is accomplished by providing a query parameter named `noLinks` to any API URI. If this query parameter is
present in the URI, its value is assumed to be `true` unless its value is `false`, which corresponds to the default
behavior where links are output. Any other value will be understood as `true`.

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

    "name" : "<the node's unescaped name>",
    "type" : "<the node's node type name>",
    "properties" : <properties representation>,
    "mixins" : <mixins representation>,
    "children" : <children representation>,
    "versions" : <versions representation>,
    "_links" : {
        "absolute": {
            "rel": "absolute",
            "href": "<An absolute URL directly usable to retrieve this node's representation>"
        },
        "versions": {
            "rel": "versions",
            "href": "<URI identifying the resource associated with this node's versions>"
        },
        "mixins": {
            "rel": "mixins",
            "href": "<URI identifying the resource associated with this node's mixins>"
        },
        "path": {
            "rel": "path",
            "href": "<URI identifying the URI to access this node by path>"
        },
        "children": {
            "rel": "children",
            "href": "<URI identifying the resource associated with this node's children>"
        },
        "parent": {
            "rel": "parent",
            "href": "<URI identifying the resource associated with this node's parent or itself if the node is the root node>"
        },
        "self": {
            "rel": "self",
            "href": "<URI identifying the resource associated with this node>"
        },
        "properties": {
            "rel": "properties",
            "href": "<URI identifying the resource associated with this node's properties>"
        },
        "type": {
            "rel": "type",
            "href": "<URI identifying the resource associated with this node's type>"
        }
    }

Note that it is possible for an API client to only request a subset of the complete structure. For example,
a client might only be interested in properties for a given call and not care about the rest of the structure.

### <a name="properties"/>Properties representation

A node's properties are gathered within a `properties` object that has the following structure:

    // other node elements...
    "properties" : {
        <for each property>
            <escaped property name> : <property representation>,
        </for each property>
        "_links" : {
            "absolute" : {
                "rel" : "absolute",
                "href" : "<An absolute URL directly usable to retrieve this properties representation>"
            },
            "parent" : {
                "rel" : "parent",
                "href" : "<URI of the resource associated with the parent node of this properties sub-resource>"
            },
            "self" : {
                "rel" : "self",
                "href" : "<URI of the resource associated with this properties resource>"
            }
        }
    },
    // other node elements...

Each property is represented by an object with the following structure:

    "name" : "<unescaped name>",
    "multiValued" : <boolean indicating whether the property is multi-valued or not>
    "reference": <boolean indicating whether the property value(s) point(s) to a node or not>
    "value" : "<value>",
    "type" : "<type>",
    "_links" : {
        "path" : {
            "rel" : "path",
            "href" : "<URI identifying the URI to access this property by path>"
        },
        "absolute" : {
            "rel" : "absolute",
            "href" : "<An absolute URL directly usable to retrieve this property's representation>"
        },
        "parent" : {
            "rel" : "parent",
            "href" : "<URI identifying the resource associated with this property's parent node>"
        },
        "self" : {
            "rel" : "self",
            "href" : "<URI identifying the resource associated with this property>"
        },
        "type" : {
            "rel" : "type",
            "href" : "<URI identifying the resource associated with this property definition>"
        }
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

Additionally, if a property is a reference and the `resolveReferences` flag is set in the URI (using a query parameter appended to the URI), 
another `references` subsection will be added to the property's representation containing basic information about the node(s) being referenced
by the property value. Each entry in the `references` object is identified using the identifier of the node being referenced. Note also that 
the `resolveReferences` flag works properly with the `includeFullChildren` one. See the example below for more details.

#### Examples

An example of the `jcr:uuid` property of a `/sites/mySite` node. `jcr:uuid` is defined by the JCR specification as
being defined by the `mix:referenceable` mixin:

    "name" : "jcr:uuid",
    "value" : "039cdef3-289a-4fee-b80e-54da0ad35195",
    "type" : "string",
    "multiValued" : false,
    "reference" : false,
    "_links" : {
        ...
        "self" : { "href" : "<basecontext>/default/en/nodes/039cdef3-289a-4fee-b80e-54da0ad35195/properties/jcr__uuid" },
        "type" : { "href" : "<basecontext>/default/en/paths/jcr__system/jcr__nodeTypes/mix__referenceable/jcr__propertyDefinition" }
        ...
    }

An example of the `jcr:mixinTypes` property on a `/sites/mySite` node.

    "name" : "jcr:mixinTypes",
    "multiValued" : true,
    "value" : ["jmix:accessControlled" , "jmix:robots"],
    "type" : "string",
    "reference" : false,
    "_links" : {
        ...
        "self" : { "href" : "<basecontext>/default/en/nodes/039cdef3-289a-4fee-b80e-54da0ad35195/properties/jcr__mixinTypes" },
        "type" : { "href" : "<basecontext>/default/en/paths/jcr__system/jcr__nodeTypes/nt__base/jcr__propertyDefinition" }
        ...
    }

An example showing how indexed, same name properties URIs are represented, here the node type associated with the
property's definition is the second property defined on the `nt:base` node type:

    "name" : "jcr:primaryType",
    "value" : "jnt:virtualsite",
    "multiValued" : true,
    "type" : "string",
    "reference" : false,
    "_links" : {
        ...
        "type" : { "href" : "<basecontext>/default/en/paths/jcr__system/jcr__nodeTypes/nt__base/jcr__propertyDefinition--2" }
        ...
    }

An example showing how a `j:defaultSite` reference property pointing to a `/sites/mySite` node on a `/sites` node is
represented, demonstrating the `target` field in the `_links` section:

    "name" : "j:defaultSite",
    "value" :  "09100a94-0714-4fb6-98de-351ad63773b2",
    "multiValued" : false,
    "type" : "weakreference",
    "reference" : true,
    "_links" : {
        ...
        "type" : { "rel" : "type", "href" : "<basecontext>/default/en/paths/jcr__system/jcr__nodeTypes/jnt__virtualsitesFolder/jcr__propertyDefinition--3" },
        "target" : { "rel" : "target", "href" : "http://api.example.org/sites/mySite" }
        ...
    }

An example showing how the node pointed at by a reference property `j:node` is resolved in the `references` subsection of the property's representation 
when the `resolveReferences` flag is used in the URI:  

    "_links": { ... },
    "references": {
        "5c82bcdc-b837-4ee0-a15a-c8d8d48a0916": {
            _links: { ... },
            name: "banner-earth.png",
            type: "jnt:file",
            path: "/sites/ACMESPACE/files/Images/Banner-home-slider/banner-earth.png",
            id: "5c82bcdc-b837-4ee0-a15a-c8d8d48a0916"
        }
    },
    "name": "j:node",
    "type": "WeakReference",
    "path": "/sites/ACMESPACE/home/main/foo/j:node",
    "multiValued": false,
    "value": "5c82bcdc-b837-4ee0-a15a-c8d8d48a0916",
    "reference": true
    
An example showing how the node pointed at by a reference property `j:node` is resolved in the `references` subsection of the property's representation 
when the `resolveReferences` flag is used in the URI, in conjunction with the `includeFullChildren` flag:  

    "_links": { ... },
    "references": {
        5c82bcdc-b837-4ee0-a15a-c8d8d48a0916: {
            _links: { ... },
            "name": "banner-earth.png",
            "type": "jnt:file",
            "path": "/sites/ACMESPACE/files/Images/Banner-home-slider/banner-earth.png",
            "mixins": { ... },
            "versions": { ... },
            "properties": { ... },
            "children": { ... },
            "id": "5c82bcdc-b837-4ee0-a15a-c8d8d48a0916"
        }
    },
    "name": "j:node",
    "type": "WeakReference",
    "path": "/sites/ACMESPACE/home/main/foo/j:node",
    "multiValued": false,
    "value": "5c82bcdc-b837-4ee0-a15a-c8d8d48a0916",
    "reference": true

### <a name="mixins"/>Mixins representation

A node's attached mixins information is gathered within a `mixins` object on the node's representation, as follows:

    // other node elements...
    "mixins" : {
        <for each mixin>
            <escaped mixin name> : <mixin representation>,
        </for each mixin>
        "_links" : {
            "absolute" : {
                "rel" : "absolute",
                "href" : "<An absolute URL directly usable to retrieve this mixins sub-resource representation>"
            },
            "parent" : {
                "rel" : "parent",
                "href" : "<URI of the resource associated with the parent node of this mixins sub-resource>"
            },
            "self" : {
                "rel" : "self",
                "href" : "<URI of the resource associated with this nmixins resource>"
            }
        }
    },
    // other node elements...

Here is the structure for a mixin representation:

    "name" : <the mixin's unescaped name>,
    "_links" : {
        "absolute" : {
            "rel" : "absolute",
            "href" : "<An absolute URL directly usable to retrieve this mixin's representation>"
        },
        "self" : {
            "rel" : "self",
            "href" : "<URI identifying the resource associated with the mixin in the context of the enclosing node>"
        },
        "type" : {
            "rel" : "type",
            "href" : "<URI identifying the resource associated with the mixin's node type>"
        }
    }

#### Examples

Given the following mixin definition:

    [jmix:robots] mixin
         extends=jnt:virtualsite
        - robots (string, textarea) = 'User-agent: *'

we would get, assuming it is attached to the `49bf6a13-96a8-480a-ae8a-2a82136d1c67` node, a representation similar to:

    "_links" : {
        "absolute" : {
           "rel" : "absolute",
           "href" : "http://localhost:8080/modules/api/jcr/v1/default/en/nodes/49bf6a13-96a8-480a-ae8a-2a82136d1c67/mixins/jmix__robots"
        },
        "self" : {
           "rel" : "self",
           "href" : "/api/jcr/v1/default/en/nodes/49bf6a13-96a8-480a-ae8a-2a82136d1c67/mixins/jmix__robots"
        },
        "type" : {
           "rel" : "type",
           "href" : "/api/jcr/v1/default/en/paths/jcr__system/jcr__nodeTypes/jmix__robots"
        }
    },
    "name" : "jmix:robots",
    "properties" : {
        "j:robots" : "String"
    },
    "type" : "jmix:robots"

To attach this mixin to an existing `49bf6a13-96a8-480a-ae8a-2a82136d1c67` node, a client would perform the following,
creating a new `jmix__robots` resource in the `mixins` collection resource, using a `PUT` request:

    PUT /api/jcr/v1/default/en/nodes/49bf6a13-96a8-480a-ae8a-2a82136d1c67/mixins/jmix__robots HTTP/1.1
    Host: api.example.org

    "properties" : {
        "j__robots" : {
            "value" : "User-agent: *"
        }
    }


### <a name="children"/>Children representation

Children of a given node are gathered within a `children` object, as follows:

    // other node elements...
    "children" : {
        <for each child>
            <escaped child name> : <child representation>,
        </for each child>
        "_links" : {
            "absolute" : {
                "rel" : "absolute",
                "href" : "<An absolute URL directly usable to retrieve this children sub-resource representation>"
            },
            "parent" : {
                "rel" : "parent",
                "href" : "<URI identifying the resource associated with the parent node>"
            },
            "self" : {
                "rel" : "self",
                "href" : "<URI identifying the resource associated with the parent's node children resource>"
            }
        }
    },
    // other node elements...

Each child is represented by an object providing only minimal information about the child: its name,
its primary node type and its associated URIs (for both associated node, node type and parent node resources):

    "name" : <unescaped child name>,
    "type" : <nodetype name>,
    "id" : <identifier of the child node>,
    "_links" : {
        "absolute": {
            "rel": "absolute",
            "href": "<An absolute URL directly usable to retrieve this node's representation>"
        },
        "path": {
            "rel": "path",
            "href": "<URI identifying the URI to access this node by path>"
        },
        "parent": {
            "rel": "parent",
            "href": "<URI identifying the resource associated with this node's parent>"
        },
        "self": {
            "rel": "self",
            "href": "<URI identifying the resource associated with this node>"
        },
        "type": {
            "rel": "type",
            "href": "<URI identifying the resource associated with this node's type>"
        }
    }
    
As of version 1.1 of the API, we've added the option to include a full representation for each child instead
of simply outputting minimal information as above. This means that now children representations can now be 
equivalent to that of nodes. This fuller representation is, however, currently limited to only one level of
hierarchy, meaning that children of children will use the regular minimal representation. This feature is 
activated by providing the `includeFullChildren` query parameter on any API URI. If present, this query
parameter is assumed to have a `true` value, unless a `false` value is explicitly provided, which corresponds
to the default behavior. Any other value is understood to be `true`.

Also as of version 1.1 of the API, we've added the option to filter children that are returned when asking for
a node's children. This is accomplished by providing the `childrenNodeTypes` query parameter which value is a
concatenation of accepted node type names, separated by a comma (','). If this query parameter is present and 
its value corresponds to a valid list of node type names, only children of the corresponding node type(s) will
be output in the node's representation.

#### Example

Below is the representation of a `tags` child element of a `/sites/mySite` node,
within the context of the enclosing node `children` element:

    // ...
    "children" : {
        "tags" : {
            "_links" : {
                "path" : {
                    "rel" : "path",
                    "href" : "/api/jcr/v1/default/en/paths/sites/mySite/tags"
                },
                "absolute" : {
                    "rel" : "absolute",
                    "href" : "http://localhost:8080/modules/api/jcr/v1/default/en/nodes/e3a6e425-0afa-490b-a319-514db66eea04"
                },
                "parent" : {
                    "rel" : "parent",
                    "href" : "/api/jcr/v1/default/en/nodes/49bf6a13-96a8-480a-ae8a-2a82136d1c67"
                },
                "self" : {
                    "rel" : "self",
                    "href" : "/api/jcr/v1/default/en/nodes/e3a6e425-0afa-490b-a319-514db66eea04"
                },
                "type" : {
                    "rel" : "type",
                    "href" : "/api/jcr/v1/default/en/paths/jcr__system/jcr__nodeTypes/jnt__tagList"
                }
            },
            "name" : "tags",
            "type" : "jnt:tagList",
            "id" : "e3a6e425-0afa-490b-a319-514db66eea04"
        },
    // ...
    "_links" : {
        "absolute" : {
            "rel" : "absolute",
            "href" : "http://localhost:8080/modules/api/jcr/v1/default/en/nodes/49bf6a13-96a8-480a-ae8a-2a82136d1c67/children"
        },
        "parent" : {
            "rel" : "parent",
            "href" : "/api/jcr/v1/default/en/nodes/49bf6a13-96a8-480a-ae8a-2a82136d1c67"
        },
        "self" : {
            "rel" : "self",
            "href" : "/api/jcr/v1/default/en/nodes/49bf6a13-96a8-480a-ae8a-2a82136d1c67/children"
        }
    },
    // ...
    
If the `includeFullChildren` flag is provided on the URI, we would get the following representation for the `tags` child:

    // ...
    "children" : {
        "tags" : {
            "_links" : { ... },
            "path": "/sites/mySite/tags",
            "name" : "tags",
            "type" : "jnt:tagList",
            "id" : "e3a6e425-0afa-490b-a319-514db66eea04",
            "mixins": { ... },
            "versions": { ... },
            "properties": { ... },
            "children": { ... } // children would only contain minimal information as "expanding" children is limited to one hierarchic level
        },
    // ...
    "_links" : {
        "absolute" : {
            "rel" : "absolute",
            "href" : "http://localhost:8080/modules/api/jcr/v1/default/en/nodes/49bf6a13-96a8-480a-ae8a-2a82136d1c67/children"
        },
        "parent" : {
            "rel" : "parent",
            "href" : "/api/jcr/v1/default/en/nodes/49bf6a13-96a8-480a-ae8a-2a82136d1c67"
        },
        "self" : {
            "rel" : "self",
            "href" : "/api/jcr/v1/default/en/nodes/49bf6a13-96a8-480a-ae8a-2a82136d1c67/children"
        }
    },
    // ...
    
Assuming we have a node `aNode` with children of type `ns:foo`, `ns:bar` and `ns:baz`, adding `?childrenNodeTypes=ns:foo,ns:bar` to the 
node's URI will only return the `ns:foo` and `ns:bar` children, omitting any `ns:baz` ones. 

#### Special considerations for same-name siblings

It is possible for a node type to specify that instances of this node type allow multiple children with the same name.
They are then identified using a 1-based index representing the relative position of the child compared to other
instances of same-named children. They are created using the `children` resource of the parent node and are appended
at the end of the parent node's children collection. Their URIs and escaped names use the `--<index>` suffix
convention we discussed previously.

For example, assuming a `foo` node allows for multiple `bar` children:

    # Adding a bar child
    PUT <basecontext>/default/en/nodes/foo/children/bar HTTP/1.1
    Host: api.example.org
    // bar content

    # Response
    HTTP/1.1 200 OK
    Content-Type: application/hal+json

    "name" : "foo",
    // ...
    "children" : {
        "bar" : {
            "name" : "bar",
            "type" : "bar:nodeType",
            // ...
        },
        // ...
    }

    # Adding a bar child
    PUT <basecontext>/default/en/nodes/foo/children/bar HTTP/1.1
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
            // ...
        },
        "bar--2" : {
            "name" : "bar",
            "type" : "bar:nodeType",
            // ...
        },
        // ...
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
            "absolute" : {
                "rel" : "absolute",
                "href" : "<An absolute URL directly usable to retrieve this versions sub-resource representation>"
            },
            "parent" : {
                "rel" : "parent",
                "href" : "<URI identifying the resource associated with this versions sub-resource's parent>"
            },
            "self" : {
                "rel" : "self",
                "href" : "<URI identifying the resource associated with this node>"
            }
        },
    },
    // other node elements...

Each version is represented as follows:

    "_links" : {
        "absolute" : {
            "rel" : "absolute",
            "href" : "<An absolute URL directly usable to retrieve this version's representation>"
        },
        "self" : {
            "rel" : "self",
            "href" : "<URI of the resource associated with this version>"
        },
        "nodeAtVersion" : {
            "rel" : "nodeAtVersion",
            "href" : "<URI corresponding to the node data (frozen node) associated with this version>"
        },
        "next" : {
            "rel" : "next",
            "href" : "<URI of the linear successor of this version if it exists (otherwise this link is skipped)>"
        },
        "previous" : {
            "rel" : "previous",
            "href" : "<URI of the linear predecessor of this version if it exists (otherwise this link is skipped)>"
        }
    },
    "name" : "<unescaped name of this version>",
    "created" : <creation time of this version>

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

### Authentication

Access to the JCR content is protected and different users will be able to see different content. It is therefore required to authenticate properly before using the API. In
fact, some errors (usually `PathNotFoundException`s) can be the result of attempting to access a node for which you don't have proper access level. There are several options to
log into Jahia Digital Factory from clients.

If you use a browser and log in, the API will use your existing session.

From a non-browser client, you have different options. You can programatically log in using the `/cms/login` URL, `POST`ing to it as follows:
`<your Jahia root context>/cms/login?doLogin=true&restMode=true&username=<user name>&password=<user password>&redirectActive=false`
For example, if you're using cURL, you would do it as follows:
`curl -i -X POST --cookie-jar cookies.txt '<Jahia DF context>/cms/login?doLogin=true&restMode=true&username=<user name>&password=<user password>&redirectActive=false'`
Note that we're using the `cookie-jar` option. This is needed to record the session information sent back by the server as we then need to provide it for each subsequent request
 using this mode.

Alternatively, you can use the `Authentication` HTTP header, using the `Basic` authentication mode. You can generate a `Basic` authentication token using your credentials and
then provide it using the `Authentication` header with each request to the API. See
[Client side Basic HTTP Authentication](http://en.wikipedia.org/wiki/Basic_access_authentication#Client_side) for more details on how to do this.

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

### Error reporting

Should an error occur during the processing of an API call, a response using an appropriate HTTP error code should be returned with a JSON body providing some context and
details about what went wrong in the form of a JSON object with the following format:

      {
         "exception": <type of the exception that occurred as a fully qualified Java exception name if available>,
         "message": <associated message if any>,
         "operation": <type of operation that triggered the error>,
         "nodeAccess": <how the node on which the error was triggered was accessed: 'byId' if using the nodes entry point or 'byPath' if the paths entry point was used>,
         "idOrPath": <identifier if nodeAccess is byId or path if nodeAccess is byPath of the node that caused the issue>,
         "subElementType": <type of sub-element requested if any>,
         "subElements": <list of requested sub-elements if any>,
         "data": <JSON data that was provided in the request>
       }

Currently the following types of operations exist: `read`, `createOrUpdate`, `delete` which map to `GET`, `PUT` and `DELETE` requests respectively and `upload` which corresponds
 to the `POST`-performed upload method.

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

#### Allowed HTTP operations

- `GET`: to retrieve the identified resource
- `PUT`: to create (if it doesn't already exist) or update the identified resource
- `DELETE`: to delete the identified resource
- `POST`: to rename a resource but leave it at the same spot in the hierarchy

#### Accepted data

`PUT` operations accept JSON representations of the objects that the method intends to update or create, meaning a `PUT` to create a property must provide
a valid JSON representation of a property, a `PUT` to update a node must provide in the body of the request a valid JSON representation of a node.
As mentioned before, this representation only needs to be partial, containing only the information that is required to complete the operation.

`DELETE` operations accept a JSON array of String identifiers of elements to be batch-deleted. This way several elements can be deleted in one single call.

#### Examples

`GET <basecontext>/default/en/nodes/` will retrieve the root node of the `default` workspace using its English version when internationalized
exists.

`GET <basecontext>/live/fr/nodes/children` will retrieve only the children of the root node in the `live` workspace using the French version.

`PUT <basecontext>/default/en/nodes/27d671f6-9c75-4604-8f81-0d1861c5e302/children/foo` with the `{"type" : "jnt:bigText", "properties" : {"text" : {"value" : "FOO!"}}}` JSON body
 data will create a new node of type `jnt:bigText` named `foo` with a `text` property set to `FOO!` and add it to the children of the node identified with the
 `27d671f6-9c75-4604-8f81-0d1861c5e302` identifier. Note that this assumes that such a child can be added on that particular node. For example, sending the same request to a
 node that doesn't accept `jnt:bigText` children will result in a `500` error response with a body similar to the following one:

      {
        "exception": "javax.jcr.nodetype.ConstraintViolationException",
        "message": "No child node definition for foo found in node /sites/ACMESPACE/home/slider-1/acme-space-demo-carousel",
        "operation": "createOrUpdate",
        "nodeAccess": "byId",
        "idOrPath": "9aa720a1-23d4-454e-ac9c-8bfdf83a8351",
        "subElementType": "children",
        "subElements": ["foo"],
        "data": {
          "type": "jnt:bigText",
          "properties": {
            "text": {
              "multiValued": false,
              "value": "FOO BAR!",
              "reference": false
            }
          }
        }
      }

This body response provides some details as to why this particular operation failed.

`PUT <basecontext>/default/en/nodes/27d671f6-9c75-4604-8f81-0d1861c5e302/properties/foo` with the `{"value" : "bar"}` JSON body data will
create (or update if it already exists) the `foo` property of the node identified by the `27d671f6-9c75-4604-8f81-0d1861c5e302` identifier
and sets its value to `bar`.

`PUT <basecontext>/default/en//nodes/eae598a3-8a41-4003-9c6b-f31018ee0e46/mixins/jmix__rating` with the
`{"properties" : {"j__lastVote": {"value": "-1"}, "j__nbOfVotes": {"value": "100"}, "j__sumOfVotes": {"value": "1000"}}}'` JSON body data will add the `jmix:rating` mixin on the
`eae598a3-8a41-4003-9c6b-f31018ee0e46` node, initializing it with the following properties values: `j:lastVote` set to `-1`,
`j:nbOfVotes` set to `100` and `j:sumOfVotes` set to `1000`. If the mixin already existed on this node then the properties would only be updated to the specified values.
Once that `jmix:rating` is added to the node, you can then modify its properties as you would do with "normal" properties.
For example, `PUT <basecontext>/default/en/nodes/eae598a3-8a41-4003-9c6b-f31018ee0e46/properties/j__sumOfVotes` with the `{"value": "3000"}` JSON body data will update the `j:sumOfVotes`
property to `3000`.

`DELETE <basecontext>/default/en/nodes/eae598a3-8a41-4003-9c6b-f31018ee0e46/mixins/jmix__rating` will remove the mixin from the node, while

`DELETE <basecontext>/default/en/nodes/eae598a3-8a41-4003-9c6b-f31018ee0e46/properties/j__sumOfVotes` will just remove the `j:sumOfVotes` property.

`DELETE <basecontext>/default/en/nodes/eae598a3-8a41-4003-9c6b-f31018ee0e46/properties` with the `["j__sumOfVotes", "j__nbOfVotes"]` will delete both `j:sumOfVotes` and
`j:nbOfVotes` properties.

`POST <basecontext>/default/en/nodes/eae598a3-8a41-4003-9c6b-f31018ee0e46/moveto/newName` will rename the `eae598a3-8a41-4003-9c6b-f31018ee0e46` node to `newName`,
leaving it at the same spot in the JCR tree.

### Operating on nodes using their path

#### URI template
`/{workspace}/{language}/paths{path: /.*}`

#### URI elements

- `paths`: path element marking access to JCR nodes from their path
- `{path: /.*}`: the path of the resource to operate one

The `path` path element should contain the absolute path to a given JCR node with optional sub-element resolution if one of the child resource
names defined in the [URI Design](#uri) section is found. Note that once a sub-element is found, the node resolution will occur up to that
sub-element and the resolution of the sub-element will happen using the next path element, all others being discarded.

#### Allowed HTTP operations

- `GET`: to retrieve the identified resource
- `PUT`: to create (if it doesn't already exist) or update the identified resource (starting from v1.1 of the API)
- `DELETE`: to delete the identified resource (starting from v1.1 of the API)
- `POST`: to upload a file as a child node of the identified resource using `multipart/form-data` content type and specifying the file to upload using the `file` parameter

#### Examples

`<basecontext>/default/en/paths/users/root/profile` resolves to the `/users/root/profile` node in the `default` workspace using the `en` language.

`<basecontext>/live/fr/paths/sites/foo/properties/bar` resolves to the French (`fr` language) version of the `bar` property of the `/sites/foo` node
in the `live` workspace.

`<basecontext>/live/fr/paths/sites/foo/properties/bar/baz/foo` also resolves to the French version of the `bar` property of the `/sites/foo` node since
 only the next path element is considered when a sub-element type if found in one of the path elements of the considered URI.

`POST <basecontext>default/en/paths/users/root/files` using `multipart/form-data` content type and specifying the file to upload using the `file` parameter will upload the
specified file to the `/users/root/files` content directory. Using cURL: `curl -i -H "Authorization:Basic cm9vdDpyb290MTIzNA==" -F file=@file.png
<basecontext>/default/en/paths/users/root/files`. The API will respond with the new node's representation, including its links and identifier so that you can further work with it.

### Retrieving nodes using their type

Version 1.1.1 of the API restricts the types endpoint to limit security exposure. It is therefore disabled by default. Its activation is controlled by the value 
of the `jahia.find.disabled` property that can be set in the `digital-factory-config/jahia/jahia.properties` properties file of your Digital Factory install. Please refer to the
Digital Factory documentation for more details.  
 
#### URI template
`/{workspace}/{language}/types/{type}`

#### URI elements

- `types`: path element marking access to JCR nodes from their type
- `{type}`: the escaped name of the type of JCR nodes to retrieve

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

#### Examples

`GET <basecontext>/default/en/types/genericnt__event?nameContains=jahia` will retrieve all the `genericnt:event` nodes which name contains `jahia`.

`GET <basecontext>/default/en/types/genericnt__event?nameContains=rest&offset=5&limit=10` will retrieve at most 10 `genericnt:event` nodes starting with the 6th one and which name
contains `rest`.

### Querying nodes

Version 1.1.1 of the API restricts the query endpoint introduced in version 1.1 to limit security exposure. It is therefore disabled by default. Its activation is 
controlled by the value of the `jahia.find.disabled` property that can be set in the `digital-factory-config/jahia/jahia.properties` properties file of your Digital Factory 
install. Please refer to the Digital Factory documentation for more details. 

#### URI template
`/{workspace}/{language}/query`

#### URI elements

- `query`: path element marking access to the query endpoint

#### Allowed HTTP operations

- `POST`: to query the JCR repository

#### Accepted data

The query endpoint accepts JSON data consisting of the following object structure:
    
    { 
        "query" : <A String representing a valid JCR-SQL2 query>,
        "limit" : <An optional Integer specifying the maximum number of to retrieve>,
        "offset": <An optional Integer specifying the starting index of the elements to retrieve to allow for pagination>
    }

As of v1.1.1 of the API, the query endpoint is disabled by default and the preferred way to query nodes is to register prepared queries that are checked by administrators of the 
server and are deemed "safe". These prepared queries are still available even when the default query endpoint is de-activated since they have been vetted. The accepted JSON input
has therefore evolved to support these prepared queries as follows:

    { 
        "query" : <An optional String representing a valid JCR-SQL2 query>,
        "queryName": <An optional String identifying a registered prepared query>,
        "parameters": <An optional array of Strings providing values for parameter placeholders (the '?' character) in the prepared query>,
        "namedParameters": <An optional dictionary of String -> Object providing values for named parameters in the prepared query>,
        "limit" : <An optional Integer specifying the maximum number of to retrieve>,
        "offset": <An optional Integer specifying the starting index of the elements to retrieve to allow for pagination>
    }

The `query` value is still supported as previously. However, it will only be taken into account if and only if the query endpoint is activated and no `queryName` value is 
provided. Otherwise, the API implementation will look for a prepared query registered under the provided `queryName` value. A prepared query can specify placeholders to 
dynamically provide values when the query is run. This is accomplished using two different means. First, for simple cases, you can use the `?` character as a placeholder for 
values to be provided later. In that case, you will need to provide a `parameters` array of values to replace these placeholders. Order is significant since placeholders
are replaced by the provided values in order, the first placeholder being replaced by the first provided value, etc. The other, and preferred, option for complex queries, is to 
use named parameters with placeholders in the form of a parameter name prefixed by a column (`:`) and preceded by a space. Parameter names can contain only alpha-numerical and 
underscore (`_`) characters. In this case, you will need to provide a `namedParameters` dictionary providing a mapping between a parameter name and its associated value. 

The prepared query will thus be interpolated using the provided values for the parameters and then limited and offset if needed.

Prepared queries are registered using your module Spring context by defining `PreparedQuery` beans. You will therefore need your module to depend on the `jcrestapi` module.
You need to provide values for the `name` and `source` properties, `name` being the name of the prepared query with which you can access it from the query endpoint and the 
`source` property being the query itself, using the appropriate placeholders (either `?` or named parameters as you see fit, but you cannot mix both in the same query). 


#### Examples

`POST <basecontext>/default/en/query` providing the following body `{query: "SELECT * FROM [nt:base]", limit: 10, offset: 1}` will result in
retrieving 10 nodes starting with the second one (i.e. bypassing the first one since the offset is not 0). Note that this query will only work if the query endpoint has been 
activated (it is disabled by default).

Assuming you've registered the following prepared query in your module's Spring context:
    
    <bean id="foo" class="org.jahia.modules.jcrestapi.api.PreparedQuery">
        <property name="name" value="foo"/>
        <property name="source" value="select * from [nt:nodeType] where [jcr:nodeTypeName] like ?"/>
    </bean>

You can execute the query using `POST <basecontext>/default/en/query` with the following body `{"queryName": "foo", "parameters": [ "nt:%" ] }` to execute the following 
interpolated query on the English version of the `default` workspace: `select * from [nt:nodeType] where [jcr:nodeTypeName] like 'nt:%'`.

If, on the other hand, you used the named parameter option:

    <bean id="foo" class="org.jahia.modules.jcrestapi.api.PreparedQuery">
        <property name="name" value="foo"/>
        <property name="source" value="select * from [nt:nodeType] where [jcr:nodeTypeName] like :nodeType"/>
    </bean>

You could run the same query using the same request providing the following body this time: `{"queryName": "foo", "namedParameters": { "nodeType": "nt:%" } }`

