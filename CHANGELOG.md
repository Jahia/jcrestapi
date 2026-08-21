# jcrestapi Changelog

## 0.0.1

* Restricted deletions in the JCR REST API to the nodes it is configured to expose.

  A deletion is now answered against the node it removes, and not only against the node the request path names. A request that removes a child is therefore refused when the API does not expose that child, whether the request removes one child or several at once. Grant the delete permission for the JCR REST API on that child, or remove the child's type from `jahia.find.nodeTypesToSkip` in `jahia.properties`.

* Restricted the JCR REST API so it can no longer write user, group or access-control settings.

  Custom code that wrote a user, group, role or permission through the JCR REST API must use the corresponding service API instead. A request that names one of these properties or mixins in its URL is answered with 403. A request that creates a node of one of these types is answered the same way. Reading is unchanged, and modifying a node of one of these types is unchanged. To adjust what the API refuses, set `jahia.api.jcr.restrictedProperties`, `jahia.api.jcr.restrictedMixins` or `jahia.api.jcr.restrictedNodeTypes` in `jahia.properties`.

* Restricted renaming in the JCR REST API to the nodes it is configured to expose.

  A rename is now answered against the node it renames, the way every other operation of this API already is. A request is refused when the API does not expose that node. Grant the move permission for the JCR REST API on that node, or remove the node's type from `jahia.find.nodeTypesToSkip` in `jahia.properties`. The rename operation is named `move`. A site that grants the whole `jcrestapi` API needs no change, and that is what the authorization configuration Jahia ships does. A site whose authorization configuration lists the operations of this API one by one must add `jcrestapi.move` to keep the rename endpoint working.

  A rename that moves the node under a different parent is answered against that parent as well. The new name is resolved as a relative path, so a name such as `../folder/name` moves the node rather than renaming it in place. Such a request is refused when the API does not expose the parent the node lands under.

* Fixed the rename route of the JCR REST API for a node whose parent is the repository root.

  `POST /modules/api/jcr/v1/{workspace}/{language}/nodes/{id}/moveto/{newName}` answered 500 for such a node, because a path it built carried an empty segment. The route now renames the node and answers 303, the way it already does for a node deeper in the tree.
