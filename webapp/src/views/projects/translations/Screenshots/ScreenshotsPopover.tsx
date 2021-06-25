import { Box, Popover, Typography } from '@material-ui/core';
import { T } from '@tolgee/react';
import { FunctionComponent } from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { ScreenshotGallery } from './ScreenshotGallery';

type KeyTranslationsDTO =
  components['schemas']['KeyWithTranslationsResponseDto'];

export interface ScreenshotsPopoverProps {
  data: KeyTranslationsDTO;
  anchorEl: Element;
  onClose: () => void;
}

export const ScreenshotsPopover: FunctionComponent<ScreenshotsPopoverProps> = (
  props
) => {
  const id = `screenshot-popover-${props.data.id}`;

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
          <ScreenshotGallery data={props.data} />
        </Box>
      </Popover>
    </>
  );
};
