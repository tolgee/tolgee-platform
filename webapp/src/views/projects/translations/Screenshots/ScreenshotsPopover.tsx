import { FunctionComponent } from 'react';
import { Box, Popover, Typography } from '@mui/material';
import { T } from '@tolgee/react';

import { ScreenshotGallery } from './ScreenshotGallery';

export interface ScreenshotsPopoverProps {
  keyId: number;
  anchorEl: Element;
  onClose: () => void;
}

export const ScreenshotsPopover: FunctionComponent<ScreenshotsPopoverProps> = (
  props
) => {
  const id = `screenshot-popover-${props.keyId}`;

  return (
    <>
      <Popover
        id={id}
        open={true}
        anchorEl={props.anchorEl}
        onClose={props.onClose}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'center',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'center',
        }}
      >
        <Box width="408px">
          <Box p={2}>
            <Typography>
              <T>translations_screenshots_popover_title</T>
            </Typography>
          </Box>
          <ScreenshotGallery keyId={props.keyId} />
        </Box>
      </Popover>
    </>
  );
};
