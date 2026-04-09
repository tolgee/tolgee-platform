import { useMemo } from 'react';
import { getTolgeeFormat } from '@tginternal/editor';

import { TranslationPlurals } from './TranslationPlurals';
import { T } from '@tolgee/react';
import { styled } from '@mui/material';
import { DirectionLocaleWrapper } from '../DirectionLocaleWrapper';
import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { TranslationVariantVisual } from 'tg.views/projects/translations/translationVisual/TranslationVariantVisual';

type QaIssueModel = components['schemas']['QaIssueModel'];

const StyledDisabled = styled(DirectionLocaleWrapper)`
  color: ${({ theme }) => theme.palette.text.disabled};
  cursor: default;
`;

type Props = {
  maxLines?: number;
  text: string | undefined;
  locale: string;
  targetLocale?: string;
  width?: number | string;
  disabled?: boolean;
  showHighlights?: boolean;
  isPlural: boolean;
  extraPadding?: boolean;
  qaIssues?: QaIssueModel[] | null;
  translationId?: number;
};

export const TranslationVisual = ({
  maxLines,
  text,
  locale,
  targetLocale,
  width,
  disabled,
  showHighlights,
  isPlural,
  extraPadding,
  qaIssues,
  translationId,
}: Props) => {
  const project = useProject();
  const value = useMemo(() => {
    return getTolgeeFormat(text || '', isPlural, !project.icuPlaceholders);
  }, [text, isPlural]);
  const openedQaIssues = useMemo(
    () => (qaIssues ?? []).filter((issue) => issue.state === 'OPEN'),
    [qaIssues]
  );

  if (disabled) {
    return (
      <StyledDisabled languageTag={locale}>
        <T keyName="translation_visual_disabled" />
      </StyledDisabled>
    );
  }

  if (!text) {
    return null;
  }

  return (
    <TranslationPlurals
      value={value}
      locale={locale}
      extraPadding={extraPadding}
      render={({ content, exampleValue, variant }) => (
        <TranslationVariantVisual
          value={value}
          variant={variant}
          qaIssues={openedQaIssues}
          maxLines={maxLines}
          width={width}
          content={content}
          exampleValue={exampleValue}
          locale={locale}
          targetLocale={targetLocale}
          showHighlights={showHighlights}
          translationId={translationId}
        />
      )}
    />
  );
};
