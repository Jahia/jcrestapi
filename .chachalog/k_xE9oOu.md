---
jcrestapi: patch
---

Fixed the Languages screen of the site settings so it saves its changes again.

The JCR REST API writes the language settings of a site again for a user who may manage the languages of that site. It still refuses the other properties that their node type declares `protected`. To let the API write another protected property, list its name in `jahia.api.jcr.additionalWritableProtectedProperties` in `jahia.properties`. Any user who may write a node can then write and delete that property, on every node type.
