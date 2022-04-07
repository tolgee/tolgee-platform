import React from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { T } from '@tolgee/react';
import { Box } from '@material-ui/core';
import {
  ActivityValue,
  getOnlyModifiedEntity,
  prepareValue,
} from '../../activityUtil';

export const KeyNameEditActivity = (props: {
  item: components['schemas']['ProjectActivityModel'];
}) => {
  const keyName = getOnlyModifiedEntity({
    item: props.item,
    entity: 'Key',
  })?.modifications?.['name'].old;

  return (
    <>
      <Box>
        <T
          parameters={{
            keyName: prepareValue(keyName),
            h: <ActivityValue />,
          }}
        >
          activity_key_delete
        </T>
      </Box>
    </>
  );
};
