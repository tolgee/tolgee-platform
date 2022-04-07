import React from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { T } from '@tolgee/react';
import { Box } from '@material-ui/core';
import { ActivityValue, prepareValue } from '../activityUtil';

export const ImportActivity = (props: {
  item: components['schemas']['ProjectActivityModel'];
}) => {
  const keyCount = props.item.counts?.['Key'];
  const translationCount = props.item.counts?.['Translation'];

  return (
    <>
      <Box>
        <T
          parameters={{
            keyCount: prepareValue(keyCount),
            translationCount: prepareValue(translationCount),
            h: <ActivityValue />,
          }}
        >
          activity_import
        </T>
      </Box>
    </>
  );
};
