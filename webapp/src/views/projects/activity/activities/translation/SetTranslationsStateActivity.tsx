import React from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { T } from '@tolgee/react';
import { Box } from '@material-ui/core';
import {
  ActivityValue,
  getOnlyModifiedEntity,
  prepareValue,
} from '../../activityUtil';

export const SetTranslationsStateActivity = (props: {
  item: components['schemas']['ProjectActivityModel'];
}) => {
  const modifiedTranslation = getOnlyModifiedEntity({
    item: props.item,
    entity: 'Translation',
  });

  const stateModification = modifiedTranslation?.modifications?.['state'];
  const newState = stateModification?.new;
  const keyName = modifiedTranslation?.relations?.['key']?.data?.['name'];

  const translationText = getOnlyModifiedEntity({
    item: props.item,
    entity: 'Translation',
  })?.description?.['text'];

  return (
    <>
      <Box>
        <T
          parameters={{
            newState: prepareValue(newState),
            keyName: prepareValue(keyName),
            translationText: prepareValue(translationText),
            h: <ActivityValue maxLength={50} />,
          }}
        >
          activity_set_translation_state
        </T>
      </Box>
    </>
  );
};
