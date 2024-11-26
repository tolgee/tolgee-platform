# Settings

## BE
* BE tests for saving
* Add liquibase changeset for customers already using namespaces, disable them only when there are none
  * Default is off, set it to on for customers who already use it
* BE tests

## FE
* Nice to have: Inline checkbox styling
* Nice to have: Hide default namespace select-box in settings if use namespaces is off
* FE tests maybe?

# Using

## FE
* Hide select boxes by the settings
* Hide

## BE
* Return all, ignore namespace if namespaces are turned off

# Cypress tests

* Cypress tests
* All usages of namespaces based on the feature turned-on
  * make it possible to make it changeable on-the-fly
