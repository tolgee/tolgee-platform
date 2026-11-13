import { useEffect, useRef } from 'react';
import { QUERY } from 'tg.constants/links';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';

import {
  useTranslationsActions,
  useTranslationsSelector,
} from 'tg.views/projects/translations/context/TranslationsContext';

export const useOpenCommentsFromUrl = (keyId: number | undefined) => {
  const [languageTag] = useUrlSearchState(QUERY.TRANSLATIONS_OPEN_COMMENTS);
  const selectedLanguages = useTranslationsSelector((c) => c.selectedLanguages);
  const { setEdit } = useTranslationsActions();
  const opened = useRef(false);

  const languageShown = Boolean(
    languageTag && selectedLanguages?.includes(languageTag)
  );

  useEffect(() => {
    if (opened.current || keyId === undefined || !languageShown) {
      return;
    }
    opened.current = true;
    setEdit({ keyId, language: languageTag, mode: 'comments' });
  }, [keyId, languageShown]);
};
