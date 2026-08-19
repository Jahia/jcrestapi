---
jcrestapi: patch
---

Restricted renaming in the JCR REST API to the nodes it is configured to expose.

A rename is now answered against the node it renames, the way every other operation of this API already is. A request is refused when the API does not expose that node. Grant the move permission for the JCR REST API on that node, or remove the node's type from `jahia.find.nodeTypesToSkip` in `jahia.properties`. The rename operation is named `move`. A site that grants the whole `jcrestapi` API needs no change, and that is what the authorization configuration Jahia ships does. A site whose authorization configuration lists the operations of this API one by one must add `jcrestapi.move` to keep the rename endpoint working.
