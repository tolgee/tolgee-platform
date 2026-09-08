/**
 * Whether an incoming `value` is a genuine external update, or the parent
 * echoing back an edit the editor emitted.
 *
 * While the user types, the parent's state trails the editor by at least one
 * render. Writing such a trailing value into the document deletes every
 * character typed since the parent last rendered.
 *
 * `emitted` holds the values the editor reported since the parent last caught
 * up; the caller clears it once `value` matches the document.
 */
export function isExternalValue(
  value: string,
  editorValue: string,
  emitted: Set<string>
): boolean {
  return editorValue !== value && !emitted.has(value);
}
