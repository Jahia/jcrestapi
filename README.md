# RESTful JCR Access

## Requirements

- Provide a simple CRUD REST API to JCR nodes
- Support only JSON as input/output format at this time
- Legacy options that are available in the current version of the REST API don't need to be ported / duplicated
- API should be accessible both from JS (directly or via frameworks) and traditional HTTP forms in browsers,
directly from any REST-able technology
- All JCR nodes should be accessible from the API: this mandates that authorization will need to be properly tackled
since with great accessibility/power comes great responsibility!

## Resources identification

Since JCR data is stored (mostly) in tree form, nodes are easily mapped to hierarchical resources identified by their
path, with the following conventions:

- mixins for a given node are accessed using the `mixins` child resource
- children for a given node are accessed directly via their path
- properties for a given node are found under the `props` child resource to distinguish them from children
- versions for a given node are found under the `versions` child resource

## Resources representation

This version of the API will use the [JSON](http://json.org) as specified by
[RFC 4627](http://www.ietf.org/rfc/rfc4627.txt) representation format with the `application/json` media type,
with the following conventions:

- `:` character is encoded by `__` in property names since `:` is a reserved character for URIs
- by default, only one level of depth in the hierarchy is retrieved for a given node. This means that reference
properties are not resolved and children are represented by a collection of links as detailed in the [dedicated
section](#children).

### <a name="children"></a> Special considerations regarding children

Since it is not practical to retrieve the full hierarchy for nodes, by default, only one level of hierarchy is
retrieved, children of a given node being represented by a collection of links, each link being a simple JSON object
containing the following name/value pairs:

| Name                 | Value                        |
| :------------------- | ---------------------------: |
| &lt;`child name`&gt; | &lt;`child resource URI`&gt; |
| `type`               | &lt;`node type`&gt;          |

If the children are ordered, the collection will be represented as a JSON array,
otherwise it will be represented by a regular JSON collection.
