import { useMemo } from 'react';
import { generatePlaceholdersStyle, getPlaceholders } from '@tginternal/editor';
import { styled, useTheme } from '@mui/material';
import { getLanguageDirection } from 'tg.fixtures/getLanguageDirection';
import { placeholderToElement } from './placeholderToElement';
import { useProject } from 'tg.hooks/useProject';

const StyledWrapper = styled('div')`
  white-space: pre-wrap;
`;

type Props = {
  content: string;
  pluralExampleValue?: number | undefined;
  locale: string;
  nested: boolean;
};

export const TranslationWithPlaceholders = ({
  content,
  pluralExampleValue,
  locale,
  nested,
}: Props) => {
  const project = useProject();
  const theme = useTheme();
  const direction = getLanguageDirection(locale);
  const placeholders = useMemo(() => {
    if (!project.icuPlaceholders) {
      return [];
    }
    return getPlaceholders(content || '', nested) || [];
  }, [content, nested]);

  const StyledPlaceholdersWrapper = useMemo(() => {
    return generatePlaceholdersStyle({
      styled,
      colors: theme.palette.placeholders,
      component: StyledWrapper,
    });
  }, [theme.palette.placeholders]);

  const chunks: React.ReactNode[] = [];
  let index = 0;
  for (const placeholder of placeholders) {
    if (placeholder.position.start !== index) {
      chunks.push(content?.substring(index, placeholder.position.start) ?? '');
    }
    index = placeholder.position.end;
    chunks.push(
      placeholderToElement({ placeholder, pluralExampleValue, key: index })
    );
  }

  if (index < (content || '').length) {
    chunks.push(content.substring(index));
  }

  return (
    <StyledPlaceholdersWrapper
      dir={direction}
      lang={locale}
      data-cy="translation-text"
    >
      {chunks}
    </StyledPlaceholdersWrapper>
  );
};
