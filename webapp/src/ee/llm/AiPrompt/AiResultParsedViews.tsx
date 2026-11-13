import React from 'react';
import { Box } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { StyledDescription } from 'tg.ee.module/llm/AiPrompt/AiResult';
import { PromptType } from 'tg.ee.module/llm/AiPrompt/PromptTypeToggle';
import { AiPlaygroundPreview } from 'tg.views/projects/translations/translationVisual/AiPlaygroundPreview';
import { TranslationVisual } from 'tg.views/projects/translations/translationVisual/TranslationVisual';
import { AiReviewPlaygroundResult } from 'tg.ee.module/aiReview/translations/AiReviewPlaygroundResult';

type AiReviewIssueModel = components['schemas']['AiReviewIssueModel'];

export const ReviewResultPreview = ({
  score,
  issues,
}: {
  score: number;
  issues?: AiReviewIssueModel[];
}) => (
  <Box display="grid" gap={1} data-cy="ai-prompt-result-review">
    <AiPlaygroundPreview>
      <AiReviewPlaygroundResult score={score} issues={issues} />
    </AiPlaygroundPreview>
  </Box>
);

export const TranslationResultPreview = ({
  json,
  isPlural,
  locale,
}: {
  json: Record<string, string | undefined>;
  isPlural: boolean;
  locale: string;
}) => (
  <Box display="grid" gap={1} data-cy="ai-prompt-result-translation">
    <AiPlaygroundPreview>
      <TranslationVisual
        text={json.output}
        isPlural={isPlural}
        locale={locale}
        maxLines={3}
        extraPadding={false}
      />
    </AiPlaygroundPreview>
    {json.contextDescription && (
      <StyledDescription>{json.contextDescription}</StyledDescription>
    )}
  </Box>
);

export function parsedResultPreview({
  promptType,
  runData,
  isPlural,
  locale,
}: {
  promptType?: PromptType;
  runData?: {
    score?: number | null;
    issues?: AiReviewIssueModel[] | null;
    parsedJson?: unknown;
  };
  isPlural: boolean;
  locale: string;
}): React.ReactNode | undefined {
  if (promptType === 'REVIEW') {
    if (runData?.score == null) {
      return undefined;
    }
    return (
      <ReviewResultPreview
        score={runData.score}
        issues={runData.issues ?? undefined}
      />
    );
  }
  const json = runData?.parsedJson as
    | Record<string, string | undefined>
    | undefined;
  if (json?.output == null) {
    return undefined;
  }
  return (
    <TranslationResultPreview json={json} isPlural={isPlural} locale={locale} />
  );
}
