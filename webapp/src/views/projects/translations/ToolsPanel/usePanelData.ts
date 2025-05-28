import { useProject } from 'tg.hooks/useProject';
import {
  useTranslationsActions,
  useTranslationsSelector,
} from '../context/TranslationsContext';
import { useMemo } from 'react';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';

export const usePanelData = () => {
  const project = useProject();
  const keyId = useTranslationsSelector((c) => c.cursor?.keyId);
  const languageTag = useTranslationsSelector((c) => c.cursor?.language);
  const activeVariant = useTranslationsSelector((c) => c.cursor?.activeVariant);
  const translations = useTranslationsSelector((c) => c.translations);
  const languages = useTranslationsSelector((c) => c.languages);
  const { setEditValueString } = useTranslationsActions();

  const keyData = useMemo(() => {
    return translations?.find((t) => t.keyId === keyId);
  }, [keyId, translations]);

  const language = useMemo(() => {
    return languages?.find((l) => l.tag === languageTag);
  }, [languageTag, languages]);

  const baseLanguage = useMemo(() => {
    return languages?.find((l) => l.base);
  }, [languages]);
  const translation = language?.tag
    ? keyData?.translations[language.tag]
    : undefined;

  const projectPermissions = useProjectPermissions();

  const dataProps = {
    project,
    keyData: keyData!,
    language: language!,
    baseLanguage: baseLanguage!,
    activeVariant: keyData?.keyIsPlural ? activeVariant! : undefined,
    setValue: setEditValueString,
    editEnabled: language
      ? (projectPermissions.satisfiesLanguageAccess(
          'translations.edit',
          language.id
        ) &&
          translation?.state !== 'DISABLED') ||
        Boolean(
          keyData?.tasks?.find(
            (t) =>
              t.languageTag === language.tag &&
              t.userAssigned &&
              t.type === 'TRANSLATE'
          )
        )
      : false,
    projectPermissions,
  };
  return dataProps;
};
