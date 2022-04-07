import React from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { T } from '@tolgee/react';
import { Box } from '@material-ui/core';
import {
  ActivityValue,
  getAllModifiedEntites,
  prepareValue,
  renderEntityLanguage,
} from '../../activityUtil';

export const SetTranslationsActivity = (props: {
  item: components['schemas']['ProjectActivityModel'];
}) => {
  const modifiedEntities = getAllModifiedEntites({
    item: props.item,
    entity: 'Translation',
  });

  const keyName = modifiedEntities?.[0].relations?.['key']?.data['name'];

  return (
    <>
      <Box>
        <T
          parameters={{
            keyName: prepareValue(keyName),
            h: <ActivityValue />,
          }}
        >
          activity_set_translation
        </T>
      </Box>
      {modifiedEntities?.map((entity) => (
        <Box key={entity.entityId}>
          <T
            parameters={{
              languageName: prepareValue(
                renderEntityLanguage(entity.relations?.['language'])
              ),
              oldValue: prepareValue(entity.modifications?.['text'].old),
              newValue: prepareValue(entity.modifications?.['text'].new),
              h: <ActivityValue maxLength={50} />,
            }}
          >
            activity_set_translation_change
          </T>
        </Box>
      ))}
    </>
  );
};
