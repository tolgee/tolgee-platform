import React from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { T } from '@tolgee/react';
import { Box } from '@material-ui/core';
import {
  ActivityValue,
  getAllModifiedEntites,
  getOnlyModifiedEntity,
  prepareValue,
  renderEntityLanguage,
} from '../../activityUtil';

export const KeyCreateActivity = (props: {
  item: components['schemas']['ProjectActivityModel'];
}) => {
  const keyName = getOnlyModifiedEntity({
    item: props.item,
    entity: 'Key',
  })?.modifications?.['name'].new;

  const createdTranslations = getAllModifiedEntites({
    item: props.item,
    entity: 'Translation',
  });

  const tags =
    (getOnlyModifiedEntity({
      item: props.item,
      entity: 'KeyMeta',
    })?.modifications?.['tags']?.new as any as string[]) || undefined;

  return (
    <>
      <Box>
        <T
          parameters={{
            keyName: prepareValue(keyName),
            h: <ActivityValue />,
          }}
        >
          activity_key_create
        </T>
      </Box>
      {createdTranslations?.map((translation) => (
        <Box key={translation.entityId}>
          <T
            parameters={{
              translatedText: prepareValue(
                translation.modifications?.['text'].new
              ),
              languageName: prepareValue(
                renderEntityLanguage(translation.relations?.['language'])
              ),
              keyName: prepareValue(keyName),
              h: <ActivityValue />,
            }}
          >
            activity_key_create_translation
          </T>
        </Box>
      ))}
      <Box>
        <T
          parameters={{
            tags: function Tags() {
              return (
                <>
                  {tags &&
                    tags.map((tag) => (
                      <ActivityValue key={tag}>{tag}</ActivityValue>
                    ))}
                </>
              );
            },
          }}
        >
          activity_created_key_with_tags
        </T>
      </Box>
    </>
  );
};
