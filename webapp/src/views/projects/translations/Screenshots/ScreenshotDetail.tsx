import React from 'react';
import { Box, Dialog } from '@mui/material';

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
      <Box display="flex">
        {screenshot && (
          <ScreenshotWithLabels showTooltips screenshot={screenshot} />
        )}
      </Box>
    </Dialog>
  );
};
