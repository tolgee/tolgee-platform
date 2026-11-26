import { useMemo } from 'react';
import { getTolgeeFormat } from '@tginternal/editor';

import { TranslationPlurals } from './TranslationPlurals';
import { T } from '@tolgee/react';
import { styled } from '@mui/material';
import { DirectionLocaleWrapper } from '../DirectionLocaleWrapper';
import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { TranslationVariantVisual } from 'tg.views/projects/translations/translationVisual/TranslationVariantVisual';
import { CopyTranslationButton } from 'tg.views/projects/translations/cell/CopyTranslationButton';
import { CELL_SHOW_ON_HOVER } from 'tg.views/projects/translations/cell/styles';

type QaIssueModel = components['schemas']['QaIssueModel'];

const StyledDisabled = styled(DirectionLocaleWrapper)`
  color: ${({ theme }) => theme.palette.text.disabled};
  cursor: default;
`;

const StyledCopyableVariant = styled('div')`
  display: grid;
  grid-template-columns: minmax(0, 1fr) 20px;
  align-items: start;
  gap: 8px;

  & > button {
    margin-top: -6px;
  }
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
  variantsCopyable?: boolean;
  active?: boolean;
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
  variantsCopyable,
  active,
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
        if (!variantsCopyable || !variant) {
          return variantVisual;
        }
        return (
          <StyledCopyableVariant>
            {variantVisual}
            {active && content && (
              <CopyTranslationButton
                text={content}
                variant={variant}
                className={CELL_SHOW_ON_HOVER}
              />
            )}
          </StyledCopyableVariant>
        );
      }}
    />
  );
};
