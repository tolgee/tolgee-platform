import React from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { T } from '@tolgee/react';
import { Box } from '@mui/material';
import {
  ActivityValue,
  getOnlyModifiedEntity,
  prepareValue,
} from '../../activityUtil';

export const KeyNameEditActivity = (props: {
  item: components['schemas']['ProjectActivityModel'];
}) => {
  const keyNameChange = getOnlyModifiedEntity({
    item: props.item,
    entity: 'Key',
  })?.modifications?.['name'];

  const oldKeyName = keyNameChange?.old;
  const newKeyName = keyNameChange?.new;
  return (
    <>
      <Box>
        <T
          parameters={{
            oldKeyName: prepareValue(oldKeyName),
            newKeyName: prepareValue(newKeyName),
            h: <ActivityValue />,
          }}
        >
          activity_key_name_edit
        </T>
      </Box>
    </>
  );
};
