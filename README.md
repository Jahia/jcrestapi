# RESTful JCR Access

## Requirements

- Provide a simple CRUD REST API to JCR nodes
- Support only JSON as input/output format at this time
- Legacy options that are available in the current version of the REST API don't need to be ported / duplicated
- API should be accessible both from JS (directly or via frameworks) and traditional HTTP forms in browsers,
directly from any REST-able technology
- All JCR nodes should be accessible from the API: this mandates that authorization will need to be properly tackled
since with great accessibility/power comes great responsibility!

## Operations

- GET: retrieve the identified resource
- PUT: add / update the identified resource
- POST: should only be used for complex operations or to create resources from a factory resource (both if needed)
- DELETE: delete the identified resource

## Resources identification

The natural match to map JCR data unto REST concepts is to use JCR nodes as resources, identified by their path,
which is made rather easy since JCR data is stored mostly in tree form.

## Resources representation

This version of the API will use the [JSON](http://json.org) representation format as specified by the
[RFC 4627](http://www.ietf.org/rfc/rfc4627.txt) with the `application/json` media type,
with specific representations for node elements. By default, only one level of depth in the hierarchy is retrieved
for a given node. This means that reference properties are not resolved to objects but left as strings and children
are represented by an array of child objects as detailed in the [dedicated section](#children). Mixins are listed in
an array named `mixins`, each mixin corresponding to an entry in that array using the simplified representation
detailed in the [Mixin representation section](#mixin) while versions are listed in an array named `versions` ( see
[Version representation section](#version) ).

### Making URIs opaque

It's important that clients don't rely on building URIs themselves as much as possible and thus,
we should strive to let clients treat them as *opaque* as possible. This does not mean that URIs need to be
unreadable. This, however, means that URIs for resources should be easily discoverable automatically by clients so
that they don't need to construct them. In order to do that, each resource that can be interacted with embeds a
`links` child object that minimally defines a `self` reference identifying the URI to use to interact with this
specific element. When appropriate, i.e. when an object represents a node, another `nodetype` link will also be
available so that clients can find out more about the node's metadata.

### Properties

Each property is represented by an object with the following structure:

    "<property name>" : {
        "value" : <value>,
        "type" : <type>,
        "links" : {
            "self" : "<URI identifying the resource associated with the property>",
            "nodetype" : "<URI identifying the resource associated with the property definition>"
        }
    }

`type` is the case-insensitive name of the JCR property type, and is one of: STRING, BINARY, LONG, DOUBLE, DATE,
BOOLEAN, NAME, PATH, REFERENCE, WEAKREFERENCE, URI, and DECIMAL.

todo: how to handle: (unstructured content)
[jnt:translation]
  - * (undefined) multiple
  - * (undefined)


### Node representation structure

    {
    <for each property>
        "<property name>" : {
            "value" : <value>,
            "type" : <type>,
            "links" : {
                "self" : "<URI identifying the resource associated with the property>",
            }
        }
    </for each property>
        "mixins" : [
        <for each mixin>
            {
                <mixin representation>
            },
        </for each mixin>
        ],
        "children" : [
        <for each child>
            {
                <child representation>
            },
        </for each child>
        ],
        "versions" : [
        <for each version>
            {
                <version representation>
            },
        </for each version>
        ],
        "links" : {
            "self" : "<URI identifying the resource associated with this node>",
            "nodetype" : "<URI identifying the resource associated with this node's type"
        }
    }

### <a name="mixin"></a> Mixin representation

Each mixin attached to a retrieved node is represented using a simplified view: an object with exactly one
name/value pair where the name is the name of the mixin and its associated value is the content model of the mixin
associated. The common `links` element completes the simplified view of the mixin.

For example, for a mixin defined as follows:

    [jmix:robots] mixin
     extends=jnt:virtualsite
    - robots (string, textarea) = 'User-agent: *'

the following representation will be used:

    {
        "jmix:robots" : {
            "robots" : "User-agent: *"
             "links" : {
                  "self" : "http://api.example.org/jcr__system/jcr__nodeTypes/jmix__robots",
                  "nodetype" : "http://api.example.org/jcr__system/jcr__nodeTypes/jmix__robots"
             }
        }
    }

Note that, since mixins *are* nodetypes, both `self` and `nodetype` links point to the same URI.


### <a name="children"></a> Children representation

Each child is represented by an object providing only minimal information about the child: its name,
its primary node type and its associated URIs (for both associated node resource and node type resource).


## URI design

- versions for a given node are found under the `versions` child resource
- mixins for a given node are accessed using the `mixins` child resource
- children for a given node are accessed directly via their path
- `:` character is encoded by `__` in property names since `:` is a reserved character for URIs
- indices of same name siblings (or properties) are denoted using the `--` prefix
- properties for a given node are found under the `props` child resource to distinguish them from children

### Examples

| Node                        | Encoded URI                 |
| :-------------------------- | --------------------------- |
| `/foo/ns:bar/ns:child[2]`   | `/foo/ns__bar/ns__child--2` |
| `mix:title` mixin of `/foo` | `/foo/mixins/mix__title`    |
| `jcr:uuid` property of `/a` | `/a/props/jcr__uuid`        |

## Examples

    # Retrieve mySite site
    GET /sites/mySite HTTP/1.1
    Host: api.example.org

    # Response
    HTTP/1.1 200 OK
    Content-Type: application/json

    {

        // ...

        "jcr:uuid" : {
            "value" : "039cdef3-289a-4fee-b80e-54da0ad35195",
            "type" : "string",
            "links" : {
                "self" : "http://api.example.org/sites/mySite/props/jcr__uuid",
                "nodetype" : "http://api.example.org/jcr__system/jcr__nodeTypes/mix__referenceable/jcr__propertyDefinition
            }
        },
        "jcr:mixinTypes" : {
            "value" : ["jmix:accessControlled" , "jmix:robots"],
            "type" : "string",
            "links" : {
                "self" : "http://api.example.org/sites/mySite/props/jcr__mixinTypes",
                "nodetype" : "http://api.example.org/jcr__system/jcr__nodeTypes/nt__base/jcr__propertyDefinition
            }
        },
        "jcr:primaryType" : {
            "value" : "jnt:virtualsite",
            "type" : "string",
            "links" : {
                "self" : "http://api.example.org/sites/mySite/props/jcr__primaryType",
                "nodetype" : "http://api.example.org/jcr__system/jcr__nodeTypes/nt__base/jcr__propertyDefinition--2
            }
        },
        "robots" : {
            "value" : "User-agent: *",
            "type" : "string",
            "links" : { "self" : "http://api.example.org/sites/mySite/props/robots" }
        },

        // ...

        "j:siteId" : {
            "value" : "2",
            "type" : "long",
            "links" : {
                "self" : "http://api.example.org/sites/mySite/props/j__siteId",
                "nodetype" : "http://api.example.org/jcr__system/jcr__nodeTypes/jnt__virtualsite/jcr__propertyDefinition--5
            }
        },
        "mixins" : [
            {
                "jmix:accessControlled" : {
                    "children" : [
                        {
                            "j:acl" : {
                                "jcr:primaryType" : "jnt:acl",
                                "links" : {
                                    "self" : "http://api.example.org/sites/mySite/j__acl",
                                    "nodetype" : "http://api.example.org/jcr__system/jcr__nodeTypes/jnt__acl"
                                }
                            }
                        }
                    ],
                    "links" : {
                        "self" : "http://api.example.org/jcr__system/jcr__nodeTypes/jmix__accessControlled"
                        "nodetype" : "http://api.example.org/jcr__system/jcr__nodeTypes/jmix__accessControlled"
                    }
                }
            },
            {
                "jmix:robots" : {
                    "robots" : "User-agent: *"
                     "links" : {
                          "self" : "http://api.example.org/jcr__system/jcr__nodeTypes/jmix__robots",
                          "nodetype" : "http://api.example.org/jcr__system/jcr__nodeTypes/jmix__robots"
                     }
                }
            }
        ],
        "children" : [
            {
                "files" : {
                    "jcr:primaryType" : "jnt:folder",
                    "links" : {
                        "self" : "http://api.example.org/sites/mySite/files",
                        "nodetype" : "http://api.example.org/jcr__system/jcr__nodeTypes/jnt__folder"
                    }
                }
            },
            {
                "portlets" : {
                    "jcr:primaryType" : "jnt:portletFolder",
                    "links" : {
                        "self" : "http://api.example.org/sites/mySite/portlets",
                        "nodetype" : "http://api.example.org/jcr__system/jcr__nodeTypes/jnt__portletFolder"
                    }
                }
            },
            {
                "groups" : {
                    "jcr:primaryType" : "jnt:groupsFolder",
                    "links" : {
                        "self" : "http://api.example.org/sites/mySite/groups",
                        "nodetype" : "http://api.example.org/jcr__system/jcr__nodeTypes/jnt__groupsFolder"
                    }
                }
            },
            {
                "contents" : {
                    "jcr:primaryType" : "jnt:contentFolder",
                    "links" : {
                        "self" : "http://api.example.org/sites/mySite/contents",
                        "nodetype" : "http://api.example.org/jcr__system/jcr__nodeTypes/jnt__contentFolder"
                    }
                }
            },
            {
                "templates" : {
                    "jcr:primaryType" : "jnt:templatesFolder",
                    "links" : {
                        "self" : "http://api.example.org/sites/mySite/templates",
                        "nodetype" : "http://api.example.org/jcr__system/jcr__nodeTypes/jnt__templatesFolder"
                    }
                }
            },
            {
                "tags" : {
                    "jcr:primaryType" : "jnt:tagList",
                    "links" : {
                        "self" : "http://api.example.org/sites/mySite/tags",
                        "nodetype" : "http://api.example.org/jcr__system/jcr__nodeTypes/jnt__tagList"
                    }
                }
            },
            {
                "permissions" : {
                    "jcr:primaryType" : "jnt:permission",
                    "links" : {
                        "self" : "http://api.example.org/sites/mySite/permissions",
                        "nodetype" : "http://api.example.org/jcr__system/jcr__nodeTypes/jnt__permission"
                    }
                }
            },
            {
                "home" : {
                    "jcr:primaryType" : "jnt:page",
                    "links" : {
                        "self" : "http://api.example.org/sites/mySite/",
                        "nodetype" : "http://api.example.org/jcr__system/jcr__nodeTypes/jnt__page"
                    }
                }
            },
            {
                "search-results" : {
                    "jcr:primaryType" : "jnt:page",
                    "links" : {
                        "self" : "http://api.example.org/sites/mySite/search-results",
                        "nodetype" : "http://api.example.org/jcr__system/jcr__nodeTypes/jnt__page"
                    }
                }
            },
            {
                "newsletter-confirmation" : {
                    "jcr:primaryType" : "jnt:page",
                    "links" : {
                        "self" : "http://api.example.org/sites/mySite/newsletter-confirmation",
                        "nodetype" : "http://api.example.org/jcr__system/jcr__nodeTypes/jnt__page"
                    }
                }
            },
            {
                "j:acl" : {
                    "jcr:primaryType" : "jnt:acl",
                    "links" : {
                        "self" : "http://api.example.org/sites/mySite/j:acl",
                        "nodetype" : "http://api.example.org/jcr__system/jcr__nodeTypes/jnt__acl"
                    }
                }
            }
         ],
         "links" : {
            "self": "http://api.example.org/sites/mySite",
            "nodetype": "http://api.example.org/jcr__system/jcr__nodeTypes/jnt__virtualsite"
        },
        "versions" : []
    }