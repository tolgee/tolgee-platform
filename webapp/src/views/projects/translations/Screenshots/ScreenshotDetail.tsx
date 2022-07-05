import React from 'react';
import { Dialog } from '@mui/material';

import { useConfig } from 'tg.hooks/InitialDataProvider';

interface ScreenshotDetailProps {
  onClose: () => void;
  fileName: string;
}

export const ScreenshotDetail: React.FunctionComponent<ScreenshotDetailProps> =
  (props) => {
    const config = useConfig();

    return (
      <Dialog
        onClose={props.onClose}
        aria-labelledby="simple-dialog-title"
        open={!!props.fileName}
        maxWidth={'xl'}
      >
        <img
          style={{ width: '100%' }}
          src={config.screenshotsUrl + '/' + props.fileName}
          alt="screenshot"
        />
      </Dialog>
    );
  };
