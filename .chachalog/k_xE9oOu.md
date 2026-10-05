---
jcrestapi: minor
---

Fixed the Languages screen of the site settings so it saves its changes again.

The JCR REST API writes the language settings of a site again for users who may manage the languages of that site, and still refuses the other properties that Jahia maintains itself. To let the API write more of these properties, list them in `jahia.api.jcr.additionalWritableProtectedProperties` in `jahia.properties`. The names you list are added to the language settings, which stay writable.
