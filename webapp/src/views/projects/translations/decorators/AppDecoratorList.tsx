import { useMemo } from 'react';
import { styled } from '@mui/material';

import { useTranslationsSelector } from '../context/TranslationsContext';
import { useProject } from 'tg.hooks/useProject';
import { AppDecoratorButton } from './AppDecoratorButton';
import { DecoratorAnchor, useAppDecorators } from './useAppDecorators';

const StyledRow = styled('span')`
  display: inline-flex;
  align-items: center;
  gap: 4px;
`;

type Props = {
  kind: DecoratorAnchor;
  keyId: number;
  keyName?: string;
  keyNamespace?: string | null;
  languageTag?: string | null;
  languageId?: number | null;
  translationId?: number | null;
};

export const AppDecoratorList = ({
  kind,
  keyId,
  keyName,
  keyNamespace,
  languageTag,
  languageId,
  translationId,
}: Props) => {
  const project = useProject();
  const translations = useTranslationsSelector((c) => c.translations);
  const languages = useTranslationsSelector((c) => c.languages);

  const visibleKeyIds = useMemo(
    () => (translations ?? []).map((t) => t.keyId),
    [translations]
  );
  const visibleLanguageTags = useMemo(
    () => (languages ?? []).map((l) => l.tag),
    [languages]
  );

  const { getDecorators } = useAppDecorators(
    project.id,
    visibleKeyIds,
    visibleLanguageTags
  );

  const decorators = useMemo(
    () =>
      getDecorators(kind, {
        keyName,
        keyNamespace: keyNamespace ?? undefined,
        keyId,
        projectId: project.id,
        languageTag: languageTag ?? undefined,
        languageId: languageId ?? undefined,
        translationId: translationId ?? undefined,
      }),
    [
      getDecorators,
      kind,
      keyName,
      keyNamespace,
      keyId,
      project.id,
      languageTag,
      languageId,
      translationId,
    ]
  );

  if (decorators.length === 0) return null;

  return (
    <StyledRow data-cy="app-decorator-list" data-cy-kind={kind}>
      {decorators.map((d) => (
        <AppDecoratorButton
          key={`${d.installId}:${d.actionKey}`}
          decorator={d}
          keyId={keyId}
          languageTag={languageTag ?? undefined}
        />
      ))}
    </StyledRow>
  );
};
