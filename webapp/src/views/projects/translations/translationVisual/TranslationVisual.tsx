import React, { useMemo } from 'react';

import { TranslationPlurals } from './TranslationPlurals';
import { T } from '@tolgee/react';
import { styled } from '@mui/material';
import { DirectionLocaleWrapper } from '../DirectionLocaleWrapper';
import { components } from 'tg.service/apiSchema.generated';
import { TranslationVariantVisual } from 'tg.views/projects/translations/translationVisual/TranslationVariantVisual';
import { useTolgeeFormat } from 'tg.views/projects/translations/translationVisual/useTolgeeFormat';

type QaIssueModel = components['schemas']['QaIssueModel'];

const StyledDisabled = styled(DirectionLocaleWrapper)`
  color: ${({ theme }) => theme.palette.text.disabled};
  cursor: default;
`;

const StyledVariantWithAction = styled('div')`
  display: grid;
  grid-template-columns: minmax(0, 1fr) 20px;
  align-items: start;
  gap: ${({ theme }) => theme.spacing(1)};
`;

type VariantActionProps = {
  variant: string;
  content: string;
};

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
  variantAction?: (props: VariantActionProps) => React.ReactNode;
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
  variantAction,
}: Props) => {
  const value = useTolgeeFormat(text, isPlural);
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
      render={({ content, exampleValue, variant }) => {
        const variantVisual = (
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
        );
        if (!variantAction || !variant) {
          return variantVisual;
        }
        return (
          <StyledVariantWithAction>
            {variantVisual}
            {variantAction({ variant, content: content ?? '' })}
          </StyledVariantWithAction>
        );
      }}
    />
  );
};
