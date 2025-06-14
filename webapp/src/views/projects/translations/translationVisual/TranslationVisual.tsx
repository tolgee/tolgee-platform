import { useMemo } from 'react';
import { getTolgeeFormat } from '@tginternal/editor';
import { LimitedHeightText } from 'tg.component/LimitedHeightText';

import { TranslationPlurals } from './TranslationPlurals';
import { TranslationWithPlaceholders } from './TranslationWithPlaceholders';
import { T } from '@tolgee/react';
import { styled } from '@mui/material';
import { DirectionLocaleWrapper } from '../DirectionLocaleWrapper';
import { useProject } from 'tg.hooks/useProject';

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
}: Props) => {
  const project = useProject();
  const value = useMemo(() => {
    return getTolgeeFormat(text || '', isPlural, !project.icuPlaceholders);
  }, [text, isPlural]);

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
        <LimitedHeightText
          maxLines={maxLines === undefined ? 3 : maxLines!}
          width={width}
          lineHeight="1.3em"
        >
          <TranslationWithPlaceholders
            content={content || ''}
            pluralExampleValue={exampleValue}
            locale={locale}
            targetLocale={targetLocale}
            nested={Boolean(variant)}
            showHighlights={showHighlights}
          />
        </LimitedHeightText>
      )}
    />
  );
};
