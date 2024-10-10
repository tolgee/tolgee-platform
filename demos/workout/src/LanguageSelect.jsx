import { useTolgee } from "@tolgee/react";

export function LanguageSelect() {
  const tolgee = useTolgee();

  return (
    <select
      value={tolgee.getLanguage()}
      onChange={(e) => tolgee.changeLanguage(e.target.value)}
    >
      <option value="en">English</option>
      <option value="hi">Hindi</option>
    </select>
  );
}
