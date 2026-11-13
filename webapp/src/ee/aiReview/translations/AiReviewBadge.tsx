import {
  AlertTriangle,
  DotsHorizontal,
  Stars01,
  XClose,
} from '@untitled-ui/icons-react';

import { components } from 'tg.service/apiSchema.generated';
import { StatusBadge, StatusBadgeTone } from 'tg.component/common/StatusBadge';
import { isScoreOnlyStatus } from 'tg.ee.module/aiReview/translations/aiReviewVisibility';

type AiReviewSummary = components['schemas']['TranslationAiReviewSummaryModel'];

type Props = {
  aiReview: AiReviewSummary;
};

// The AI review score in the translation cell: green when the AI marked the translation as reviewed,
// orange below the threshold, outlined grey for score only, grey otherwise.
export const AiReviewBadge = ({ aiReview }: Props) => {
  const { status, score, lastFailed, queued } = aiReview;
  const scoreOnly = isScoreOnlyStatus(status);
  const tone: StatusBadgeTone =
    status === 'APPROVED'
      ? 'success'
      : status === 'BELOW_THRESHOLD'
      ? 'warning'
      : 'neutral';

  return (
    <StatusBadge
      tone={tone}
      variant={scoreOnly ? 'outlined' : 'tinted'}
      icon={<Stars01 />}
      data-cy="ai-review-badge"
      data-cy-status={status}
      data-cy-last-failed={String(Boolean(lastFailed))}
      data-cy-queued={String(Boolean(queued))}
    >
      {status === 'OVERRIDDEN' && <XClose />}
      {status !== 'FAILED' && status !== 'OVERRIDDEN' && score}
      {queued && <DotsHorizontal />}
      {/* One failure icon, whether the review failed or only its last try did. */}
      {(status === 'FAILED' || lastFailed) && <AlertTriangle />}
    </StatusBadge>
  );
};
