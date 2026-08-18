---
jcrestapi: patch
---

Restricted batch deletions in the JCR REST API to the nodes it is configured to expose.

A request that deletes several nodes at once now gets the same answer as a request that deletes them one at a time. A batch deletion that starts returning a not-found error after the upgrade targets a node outside that configuration. Grant the delete permission for the JCR REST API on that node, or remove the node's type from `jahia.find.nodeTypesToSkip` in `jahia.properties`.

Each name sent in the request body must also name a direct child of the node the request targets. A name that carries a path, such as `folder/child`, is refused. An integration that deleted a deeper node that way must send its request on the parent of that node instead.
