package io.tolgee.model.enums

enum class BranchKeyMergeChangeType(value: String) {
  ADD("ADD"),
  UPDATE("UPDATE"),
  DELETE("DELETE"),
  CONFLICT("CONFLICT"),
  SKIP("SKIP"),
}
