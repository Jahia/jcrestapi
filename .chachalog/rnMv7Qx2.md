---
jcrestapi: patch
---

Fixed the rename route of the JCR REST API for a node whose parent is the repository root.

`POST /modules/api/jcr/v1/{workspace}/{language}/nodes/{id}/moveto/{newName}` answered 500 for such a node, because a path it built carried an empty segment. The route now renames the node and answers 303, the way it already does for a node deeper in the tree.
