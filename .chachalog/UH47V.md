---
jcrestapi: patch
---

Restricted the JCR REST API so it can no longer write user, group or access-control settings.

Custom code that wrote a user, group, role or permission through the JCR REST API must use the corresponding service API instead. A request that names one of these properties or mixins in its URL is answered with 403. A request that creates a node of one of these types is answered the same way. Reading is unchanged, and modifying a node of one of these types is unchanged. To adjust what the API refuses, set `jahia.api.jcr.restrictedProperties`, `jahia.api.jcr.restrictedMixins` or `jahia.api.jcr.restrictedNodeTypes` in `jahia.properties`.
