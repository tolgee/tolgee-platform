import React from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { T } from '@tolgee/react';
import { Box } from '@material-ui/core';
import {
  ActivityValue,
  getOnlyModifiedEntity,
  prepareValue,
} from '../../../activityUtil';

export const ScreenshotAddActivity = (props: {
  item: components['schemas']['ProjectActivityModel'];
}) => {
  const keyName = getOnlyModifiedEntity({
    item: props.item,
    entity: 'Screenshot',
  })?.relations?.['key']?.data?.['name'];

  return (
    <>
      <Box>
        <T
          parameters={{
            keyName: prepareValue(keyName),
            h: <ActivityValue />,
          }}
        >
          activity_add_screenshot
        </T>
      </Box>
    </>
  );
};
