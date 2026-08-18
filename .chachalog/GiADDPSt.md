---
jcrestapi: patch
---

Restricted deletions in the JCR REST API to the nodes it is configured to expose.

A deletion is now answered against the node it removes, and not only against the node the request path names. A request that removes a child is therefore refused when the API does not expose that child, whether the request removes one child or several at once. Grant the delete permission for the JCR REST API on that child, or remove the child's type from `jahia.find.nodeTypesToSkip` in `jahia.properties`.
