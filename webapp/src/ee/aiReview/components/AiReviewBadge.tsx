import { useRef, useState } from 'react';
import { ButtonBase, styled, Tooltip } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { useProject } from 'tg.hooks/useProject';
import { AiReviewBadgeProps } from 'eeSetup/EeModuleType';
import { useAiReviewEnabled } from 'tg.ee.module/aiReview/hooks/useAiReviewEnabled';
import { useAiReviewOutcomeLabel } from 'tg.ee.module/aiReview/hooks/useAiReviewLabels';
import { AiReviewDetailPopover } from 'tg.ee.module/aiReview/components/AiReviewDetailPopover';
import { AiReviewScoreChip } from 'tg.ee.module/aiReview/components/AiReviewScoreChip';
import { isObsoleteForState } from 'tg.ee.module/aiReview/utils/obsoleteOutcome';

const StyledButton = styled(ButtonBase)`
  border-radius: 10px;
  display: flex;
`;

export const AiReviewBadge = ({
  aiReview,
  translationId,
  translationState,
  keyId,
  languageTag,
  actionsEnabled,
  className,
  style,
}: AiReviewBadgeProps) => {
  const { t } = useTranslate();
  const project = useProject();
  const { available } = useAiReviewEnabled();
  const outcomeLabel = useAiReviewOutcomeLabel();
  const anchorRef = useRef<HTMLButtonElement>(null);
  const [open, setOpen] = useState(false);

  if (
    !available ||
    !aiReview ||
    translationId === undefined ||
    isObsoleteForState(aiReview.outcome, translationState)
  ) {
    return null;
  }

  const tooltip =
    aiReview.score != null
      ? t('ai_review_badge_tooltip', '{status} · score {score}', {
          status: outcomeLabel(aiReview.outcome, aiReview.dryRun),
          score: aiReview.score,
        })
      : outcomeLabel(aiReview.outcome, aiReview.dryRun);

  return (
    <>
      <Tooltip title={open ? '' : tooltip} disableInteractive>
        <StyledButton
          ref={anchorRef}
          className={className}
          style={style}
          onClick={(e) => {
            e.stopPropagation();
            setOpen(true);
          }}
          data-cy="translations-cell-ai-review-badge"
          data-cy-outcome={aiReview.outcome}
          data-cy-score={aiReview.score ?? ''}
        >
          <AiReviewScoreChip
            outcome={aiReview.outcome}
            score={aiReview.score}
            dryRun={aiReview.dryRun}
          />
        </StyledButton>
      </Tooltip>
      {open && anchorRef.current && (
        <AiReviewDetailPopover
          anchorEl={anchorRef.current}
          onClose={() => setOpen(false)}
          projectId={project.id}
          translationId={translationId}
          keyId={keyId}
          languageTag={languageTag}
          actionsEnabled={actionsEnabled}
        />
      )}
    </>
  );
};
