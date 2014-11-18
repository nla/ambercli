AmberCLI
========

A small command-line interface for exploring AmberDB.

Usage
-----

Given a blob id you can look up the work and copies path that references it:

```
amber> blob nla.blob-80901
Work nla.obj-7614582 [Work]
  Work nla.obj-7617702 [Page]
    Copy nla.obj-7617728 [m]: nla.blob-80901
```

You can view the properties of works and copies:

```
amber> info nla.obj-7617728
copyRole: m
copyType: d
currentVersion: Yes
materialType: Unknown
type: Copy
```

You can display the work/copy tree structure from a root work down:

```
amber> tree nla.obj-7614582
Work nla.obj-7614582 [Work]
  Work nla.obj-7617702 [Page]
    Copy nla.obj-7617728 [m]: nla.blob-80901
  Work nla.obj-7616804 [Page]
    Copy nla.obj-7616820 [m]: nla.blob-80871
  ...
```

Finally you can drop into a JavaScript shell which lets you call any method
you like on the database:

```
amber> js
js> x = db.findWork("nla.obj-7617824")
vertex id:761782 {bibId:1299, bibLevel:Part, digitalStatusDate:Mon Nov 17 10:46:35 EST 2014, copyrightPolicy:Out of Copyright, form:Book, accessConditions:Unrestricted, subType:page, collection:nla.map, title:Zum Thema Sprache und Logik : Ergebnisse einer interdisziplinaren Diskussion, digitalStatus:Digitised, type:Page}-761813 start:761865 end:0
js> x.countCopies()
1
js> x.getParent().getObjId()
nla.obj-7614582
```
