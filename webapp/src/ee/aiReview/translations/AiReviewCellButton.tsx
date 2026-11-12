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
        // The badge is wider than an icon: the button takes its width, and the badge keeps 12px to the icon before it, like icons do.
        sx={{
          width: 'auto',
          ml: 0,
          pl: 0,
          pr: 1,
          // The badge shows the hover itself (see AiReviewBadge), so no grey circle behind it.
          '&:hover': { background: 'transparent' },
        }}
        disableRipple
        ref={buttonRef}
        onClick={() => setOpen(true)}
        tooltip={
          aiReview.queued
            ? t('translation_cell_ai_review_queued', 'AI review in progress')
            : t('translation_cell_ai_review', 'AI review')
        }
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
        />
      )}
    </>
  );
};
