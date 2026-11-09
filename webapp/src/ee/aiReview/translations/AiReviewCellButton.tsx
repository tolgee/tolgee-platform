import { useRef, useState } from 'react';
import { useTranslate } from '@tolgee/react';

import { AiReviewCellButtonProps } from 'eeSetup/EeModuleType';
import { ControlsButton } from 'tg.views/projects/translations/cell/ControlsButton';

import { AiReviewBadge } from 'tg.ee.module/aiReview/translations/AiReviewBadge';
import { AiReviewPopover } from 'tg.ee.module/aiReview/translations/AiReviewPopover';

export const AiReviewCellButton = ({
  aiReview,
  translationId,
  state,
  stateChangeEnabled,
  style,
  className,
}: AiReviewCellButtonProps) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);
  const buttonRef = useRef<HTMLButtonElement>(null);

  return (
    <>
      <ControlsButton
        style={style}
        className={className}
        ref={buttonRef}
        onClick={() => setOpen(true)}
        tooltip={t('translation_cell_ai_review', 'AI review')}
        data-cy="translations-cell-ai-review-button"
      >
        <AiReviewBadge aiReview={aiReview} />
      </ControlsButton>
      {open && buttonRef.current && (
        <AiReviewPopover
          anchorEl={buttonRef.current}
          onClose={() => setOpen(false)}
          translationId={translationId}
          aiReview={aiReview}
          state={state}
          stateChangeEnabled={stateChangeEnabled}
        />
      )}
    </>
  );
};
