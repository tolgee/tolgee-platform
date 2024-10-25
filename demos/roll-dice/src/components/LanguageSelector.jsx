import { useTolgee } from "@tolgee/react";
import { useTranslate } from '@tolgee/react';

export function LanguageSelector() {
  const { t } = useTranslate();

  const tolgee = useTolgee(["language"]);
  return (
      <div className="flex items-center gap-2 text-sm">
        <p>{t('change_lang','Change Language')}:</p>
        <select
          className="outline-none bg-inherit text-sm"
          value={tolgee.getLanguage()}
          onChange={(e) => tolgee.changeLanguage(e.target.value)}
        >
          <option className="dark-text" value="en">
            English
          </option>
          <option className="dark-text" value="hi-IN">
            Hindi
          </option>
          <option className="dark-text" value="fr">
            French
          </option>
          <option className="dark-text" value="de-DE">
            Germany
          </option>
        </select>
      </div>
  );
}
