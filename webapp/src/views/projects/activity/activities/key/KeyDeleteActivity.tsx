import React from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { T } from '@tolgee/react';
import { Box } from '@material-ui/core';
import {
  ActivityValue,
  getOnlyModifiedEntityModification,
  prepareValue,
} from '../../activityUtil';

export const KeyDeleteActivity = (props: {
  item: components['schemas']['ProjectActivityModel'];
}) => {
  const keyName = getOnlyModifiedEntityModification({
    item: props.item,
    entity: 'Key',
    field: 'name',
  })?.old;

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
