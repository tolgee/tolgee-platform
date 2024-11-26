# BE

* Add new column for file detected namespace
  * Keep namespace column empty if we don't use namespaces
  * decide whether to fill namespace column or not
  * import namespaces fail for API calls, UI doesn't send namespaces anymore
* show warning when detected namespaces are not empty

# FE

* remove namespaces when importing
  * add automated tests for import

# Cypress tests

* All usages of namespaces based on the feature turned-on
  * make it possible to make it changeable on-the-fly

# Other

* Update figma plugin based on the `useNamespaces` value