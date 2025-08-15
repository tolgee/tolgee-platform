import { type components } from "./schema.generated";

export type PartialKey = { keyName: string; namespace?: string };

type SimpleImportConflictResult =
  components["schemas"]["SimpleImportConflictResult"];

export function renderKey(key: PartialKey, note?: string) {
  const namespace = key.namespace ? ` ${`(namespace: ${key.namespace})`}` : "";
  const renderedNote = note ? ` ${note}` : "";
  return `${key.keyName}${namespace}${renderedNote}`;
}

export function getUnresolvedConflictsMessage(
  translations: SimpleImportConflictResult[]
) {
  const someOverridable = Boolean(translations.find((c) => c.isOverridable));
  const result = [""];

  result.push(`🟡 Some translations cannot be updated:`);
  translations.forEach((c) => {
    result.push(
      renderKey(
        { keyName: c.keyName, namespace: c.keyNamespace },
        `${c.language}` + (c.isOverridable ? " (overridable)" : "")
      )
    );
  });
  result.push("");
  if (someOverridable) {
    result.push(
      "HINT: Overridable translations can be updated with the `overrideMode: ALL`"
    );
    result.push("");
  }
  return result.join("\n");
}

export function printUnresolvedConflicts(
  translations: SimpleImportConflictResult[]
) {
  console.log(getUnresolvedConflictsMessage(translations));
}
