import React from 'react';
import { Dialog } from '@mui/material';

import { useConfig } from 'tg.globalContext/helpers';

interface ScreenshotDetailProps {
  onClose: () => void;
  fileName: string;
  open: boolean;
}

export const ScreenshotDetail: React.FunctionComponent<ScreenshotDetailProps> =
  (props) => {
    const config = useConfig();

    return (
      <Dialog
        onClose={props.onClose}
        aria-labelledby="simple-dialog-title"
        open={props.open}
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
