import { useMemo } from 'react';
import {
  generatePlaceholdersStyle,
  getPlaceholders,
  Placeholder,
  Position,
} from '@tginternal/editor';
import { styled, useTheme } from '@mui/material';
import { getLanguageDirection } from 'tg.fixtures/getLanguageDirection';
import { placeholderToElement } from './placeholderToElement';
import { useProject } from 'tg.hooks/useProject';
import { useGlossaryTermHighlights } from 'tg.ee';
import { GlossaryTermHighlightModel } from '../../../../eeSetup/EeModuleType';
import { GlossaryHighlight } from 'tg.views/projects/translations/translationVisual/GlossaryHighlight';
import { QaIssueHighlight } from 'tg.ee';
import { components } from 'tg.service/apiSchema.generated';

type QaIssueModel = components['schemas']['QaIssueModel'];

const StyledWrapper = styled('div')`
  white-space: pre-wrap;
`;

type Props = {
  content: string;
  pluralExampleValue?: number | undefined;
  locale: string;
  targetLocale?: string;
  nested: boolean;
  showHighlights?: boolean;
  qaIssues?: QaIssueModel[];
  translationId?: number;
};

type Modifier = {
  position: Position;
  placeholder?: Placeholder;
  highlight?: GlossaryTermHighlightModel;
  qaIssue?: QaIssueModel;
};

function isOverlapping(a: Position, b: Position): boolean {
  return a.start < b.end && a.end > b.start;
}

function sortModifiers(
  placeholders: Placeholder[],
  highlights: GlossaryTermHighlightModel[],
  qaIssues: QaIssueModel[],
  contentLength: number
): Modifier[] {
  let modifiers: Modifier[] = placeholders.map((placeholder) => ({
    position: placeholder.position,
    placeholder: placeholder,
  }));

  highlights.forEach((highlight) => {
    const overlappingModifiers = modifiers.filter(({ position }) =>
      isOverlapping(position, highlight.position)
    );

    // Add non-overlapping highlights
    if (overlappingModifiers.length === 0) {
      modifiers.push({
        position: highlight.position,
        highlight: highlight,
      });
      return;
    }

    // If there is an overlap with only shorter highlights, replace them with the longer one
    const highlightLength = highlight.position.end - highlight.position.start;
    const canReplaceShorterHighlights = overlappingModifiers.every(
      (modifier) =>
        modifier.highlight &&
        modifier.position.end - modifier.position.start < highlightLength
    );

    if (!canReplaceShorterHighlights) {
      return;
    }

    modifiers = modifiers.filter(
      ({ position }) => !isOverlapping(position, highlight.position)
    );
    modifiers.push({
      position: highlight.position,
      highlight: highlight,
    });
  });

  // Add QA issue highlights (skip issues with no position)
  qaIssues.forEach((issue) => {
    if (issue.positionStart == null || issue.positionEnd == null) {
      return;
    }
    const issuePosition: Position = {
      start: issue.positionStart,
      end: issue.positionEnd,
    };
    const hasOverlap = modifiers.some(({ position }) =>
      isOverlapping(position, issuePosition)
    );
    if (!hasOverlap) {
      modifiers.push({
        position: issuePosition,
        qaIssue: issue,
      });
    }
  });

  return modifiers
    .filter(
      ({ position }) => position.start <= contentLength && position.end >= 0
    )
    .map((modifier) => ({
      ...modifier,
      position: {
        start: Math.max(0, modifier.position.start),
        end: Math.min(contentLength, modifier.position.end),
      },
    }))
    .sort((a, b) => a.position.start - b.position.start);
}

export const TranslationWithPlaceholders = ({
  content,
  pluralExampleValue,
  locale,
  targetLocale,
  nested,
  showHighlights,
  qaIssues = [],
  translationId,
}: Props) => {
  const project = useProject();
  const theme = useTheme();
  const direction = getLanguageDirection(locale);
  const text = content || '';
  const placeholders = useMemo(() => {
    if (!project.icuPlaceholders) {
      return [];
    }
    return getPlaceholders(text, nested) || [];
  }, [content, nested]);

  const glossaryTerms = useGlossaryTermHighlights({
    text,
    languageTag: locale,
    enabled: showHighlights ?? false,
  });

  const modifiers = sortModifiers(
    placeholders,
    glossaryTerms,
    qaIssues,
    text.length
  );

  const StyledPlaceholdersWrapper = useMemo(() => {
    return generatePlaceholdersStyle({
      styled,
      colors: theme.palette.placeholders,
      component: StyledWrapper,
    });
  }, [theme.palette.placeholders]);

  const chunks: React.ReactNode[] = [];
  let index = 0;
  for (const modifier of modifiers) {
    if (modifier.position.start !== index) {
      chunks.push(text.substring(index, modifier.position.start));
    }
    index = modifier.position.end;
    const segmentText = text.substring(
      modifier.position.start,
      modifier.position.end
    );
    if (modifier.placeholder) {
      chunks.push(
        placeholderToElement({
          placeholder: modifier.placeholder,
          pluralExampleValue,
          key: index,
        })
      );
    } else if (modifier.highlight) {
      chunks.push(
        <GlossaryHighlight
          key={`glossary-${modifier.position.start}-${modifier.highlight.value}`}
          text={segmentText}
          term={modifier.highlight.value}
          languageTag={locale}
          targetLanguageTag={targetLocale}
        />
      );
    } else if (modifier.qaIssue && translationId != null) {
      chunks.push(
        <QaIssueHighlight
          key={`qa-${modifier.position.start}-${modifier.qaIssue.type}`}
          text={segmentText}
          translationText={text}
          issue={modifier.qaIssue}
          translationId={translationId}
        />
      );
    }
  }

  if (index < text.length) {
    chunks.push(text.substring(index));
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
