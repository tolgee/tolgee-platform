import React from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { T } from '@tolgee/react';
import { Box } from '@material-ui/core';
import {
  ActivityValue,
  getAllModifiedEntites,
  prepareValue,
} from '../../activityUtil';

export const KeyDeleteActivity = (props: {
  item: components['schemas']['ProjectActivityModel'];
}) => {
  const modifiedKeys = getAllModifiedEntites({
    item: props.item,
    entity: 'Key',
  });

  return (
    <>
      {modifiedKeys?.map((key) => (
        <Box key={key.entityId}>
          <T
            parameters={{
              keyName: prepareValue(key.modifications?.['name'].old),
              h: <ActivityValue />,
            }}
          >
            activity_key_delete
          </T>
        </Box>
      ))}
    </>
  );
};
