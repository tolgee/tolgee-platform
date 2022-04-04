import React from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { T } from '@tolgee/react';
import { Box } from '@material-ui/core';
import {
  ActivityValue,
  getOnlyModifiedEntityModification,
  prepareValue,
} from '../../activityUtil';

export const SetTranslationsActivity = (props: {
  item: components['schemas']['ProjectActivityModel'];
}) => {
  const keyName = props.item.meta?.['keyName'];
  const languageTag = props.item.meta?.['languageTag'];

  const textModification = getOnlyModifiedEntityModification({
    item: props.item,
    entity: 'Translation',
    field: 'text',
  });

  const oldValue = textModification?.old;
  const newValue = textModification?.new;

  return (
    <>
      <Box>
        <T
          parameters={{
            keyName: prepareValue(keyName),
            languageTag: prepareValue(languageTag),
            h: <ActivityValue />,
          }}
        >
          activity_set_translation
        </T>
      </Box>
      <Box>
        <T
          parameters={{
            oldValue: prepareValue(oldValue),
            newValue: prepareValue(newValue),
            h: <ActivityValue maxLength={50} />,
          }}
        >
          activity_set_value_from_to
        </T>
      </Box>
    </>
  );
};
