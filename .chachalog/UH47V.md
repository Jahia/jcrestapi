---
jcrestapi: patch
---

Restricted the JCR REST API so it can no longer write user, group or access-control settings.

Custom code that wrote a user, group, role or permission property through the JCR REST API must use the corresponding service API instead. A request that names one of these properties or mixins in its URL is answered with 403. The restricted names are listed in `jahia.api.jcr.restrictedProperties` and `jahia.api.jcr.restrictedMixins` in `jahia.properties`, and you can adjust either list.
