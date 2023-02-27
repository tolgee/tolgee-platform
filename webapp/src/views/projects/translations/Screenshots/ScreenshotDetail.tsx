import React from 'react';
import { Dialog } from '@mui/material';

import {
  ScreenshotProps,
  ScreenshotWithLabels,
} from 'tg.component/ScreenshotWithLabels';

interface ScreenshotDetailProps {
  onClose: () => void;
  open: boolean;
  screenshot: ScreenshotProps | undefined;
  highlightedKeyId: number;
}

export const ScreenshotDetail: React.FC<ScreenshotDetailProps> = ({
  onClose,
  open,
  screenshot,
}) => {
  return (
    <Dialog
      onClose={onClose}
      aria-labelledby="simple-dialog-title"
      open={open}
      maxWidth="xl"
    >
      <div>
        {screenshot && (
          <ScreenshotWithLabels showTooltips screenshot={screenshot} />
        )}
      </div>
    </Dialog>
  );
};
