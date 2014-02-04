# RESTful JCR Access

## Requirements

- Provide a simple CRUD REST API to JCR nodes
- Support only JSON as input/output format at this time
- Legacy options that are available in the current version of the REST API don't need to be ported / duplicated
- API should be accessible both from JS (directly or via frameworks) and _optionally_ traditional HTTP forms in
browsers, directly from any REST-able technology
- All JCR nodes should be accessible from the API: this mandates that authorization will need to be properly tackled
since with great accessibility/power comes great responsibility!

## TODO:

- Improve cache control using ETag (for simple node GETs) and using Response.cacheControl method instead of filter.

## Resources identification

The natural match to map JCR data unto REST concepts is to use JCR nodes as resources, identified by their path,
which is made rather easy since JCR data is stored mostly in tree form.

## <a name="uri"/>URI design

- versions for a given node are found under the `versions` child resource
- mixins for a given node are accessed using the `mixins` child resource
- children for a given node are accessed directly via their path or using the `children` child resource
- `:` character is encoded by `__` in property names since `:` is a reserved character for URIs
- indices of same name siblings are denoted using the `--` prefix
- properties for a given node are found under the `properties` child resource to distinguish them from children

### Examples

| Node                        | Encoded URI                 |
| :-------------------------- | --------------------------- |
| `/foo/ns:bar/ns:child[2]`   | `/foo/ns__bar/ns__child--2` |
| `mix:title` mixin of `/foo` | `/foo/mixins/mix__title`    |
| `jcr:uuid` property of `/a` | `/a/properties/jcr__uuid`        |


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
specific element. When appropriate, another `type` link will also be available so that clients can find out more about the node's metadata. Specific objects might add more link types when appropriate.

To sum up, the `_links` section will look similarly to the following example:

    "_links" : {
        "self" : { "href" : "<URI identifying the associated resource>" },
        "type" : { "href" : "<URI identifying the resource associated with the resource type>"
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
            "self" : { "href" : "<URI identifying the resource associated with the parent's node properties resource>" }
        }
    },
    // other node elements...

Each property is represented by an object with the following structure:

    "name" : <unescaped name>,
    "multiple" : <boolean indicating whether the property has multiple values or not>
    "value" : <value>,
    "type" : <type>,
    "_links" : {
        "self" : { "href" : "<URI identifying the resource associated with the property>" },
        "type" : { "href" : "<URI identifying the resource associated with the property definition>" }
    }

`type` is the case-insensitive name of the JCR property type, and is one of: `STRING`, `BINARY`, `LONG`, `DOUBLE`,
`DATE`, `BOOLEAN`, `NAME`, `PATH`, `REFERENCE`, `WEAKREFERENCE`, `URI`, and `DECIMAL`.

`multiple` is optional, if it's not present in the returned representation, it is assumed to be `false`,
meaning that the property only has a single value. Having this field allows for easier processing of properties on
the client side without having to examine the property's definition.

If a property is of type `PATH`, `REFERENCE` or `WEAKREFERENCE`, an additional `target` link is added to the `_links`
subsection, providing the URI identifying the resource identified by the path or reference value of the property.

#### Examples

An example of the `jcr:uuid` property of a `/sites/mySite` node. `jcr:uuid` is defined by the JCR specification as
being defined by the `mix:referenceable` mixin:

    "name" : "jcr:uuid",
    "value" : "039cdef3-289a-4fee-b80e-54da0ad35195",
    "type" : "string",
    "_links" : {
        "self" : { "href" : "http://api.example.org/sites/mySite/properties/jcr__uuid" },
        "type" : { "href" : "http://api.example.org/jcr__system/jcr__nodeTypes/mix__referenceable/jcr__propertyDefinition" }
    }

An example of the `jcr:mixinTypes` property on a `/sites/mySite` node. Note the `multiple` field:

    "name" : "jcr:mixinTypes",
    "multiple" : true,
    "value" : ["jmix:accessControlled" , "jmix:robots"],
    "type" : "string",
    "_links" : {
        "self" : { "href" : "http://api.example.org/sites/mySite/properties/jcr__mixinTypes" },
        "type" : { "href" : "http://api.example.org/jcr__system/jcr__nodeTypes/nt__base/jcr__propertyDefinition" }
    }

An example showing how indexed, same name properties URIs are represented, here the node type associated with the
property's definition is the second property defined on the `nt:base` node type:

    "name" : "jcr:primaryType",
    "value" : "jnt:virtualsite",
    "type" : "string",
    "_links" : {
        "self" : { "href" : "http://api.example.org/sites/mySite/properties/jcr__primaryType" },
        "type" : { "href" : "http://api.example.org/jcr__system/jcr__nodeTypes/nt__base/jcr__propertyDefinition--2" }
    }

An example showing how a `j:defaultSite` reference property pointing to a `/sites/mySite` node on a `/sites` node is
represented, demonstrating the `target` field in the `_links` section:

    "name" : "j:defaultSite",
    "value" :  "09100a94-0714-4fb6-98de-351ad63773b2",
    "type" : "weakreference",
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
            "self" : { "href" : "<URI identifying the resource associated with the parent's node mixins resource>" }
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
            "self" : { "href" : "<URI identifying the resource associated with the parent's node children resource>" }
        }
    },
    // other node elements...

Each child is represented by an object providing only minimal information about the child: its name,
its primary node type and its associated URIs (for both associated node resource and node type resource):

    "name" : <unescaped child name>,
    "type" : <nodetype name>,
    "_links" : {
        "self" : { "href" : "<URI identifying the resource associated with the child's node>" },
        "type" : { "href" : "<URI identifying the resource associated with the child's node type>" }
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

## Operations

- GET: retrieve the identified resource
- PUT: add / update the identified resource
- POST: should only be used for complex operations or to create resources from a factory resource (both if needed)
- DELETE: delete the identified resource

### Special provision for PUT and POST operations

Nodes and properties representations can be quite complex with a potentially graph-like structure. Moreover,
many of the exposed data is actually read-only (node type information, links...) or is or can be derived from the
node type information. It would therefore be inefficient to require client code to pass all that information to the
API during creation or update of resources. We adopt the convention that only the information that is either required
 or is being changed as a result of the operation is passed along to the API. This results in minimal effort on the
 client side and has the added benefit of reducing the amount of network chatter.