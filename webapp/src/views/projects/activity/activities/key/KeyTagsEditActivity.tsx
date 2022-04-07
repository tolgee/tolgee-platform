import React from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { T } from '@tolgee/react';
import { Box } from '@material-ui/core';
import {
  ActivityValue,
  getOnlyModifiedEntity,
  prepareValue,
} from '../../activityUtil';

export const KeyTagsEditActivity = (props: {
  item: components['schemas']['ProjectActivityModel'];
}) => {
  const keyName = getOnlyModifiedEntity({
    item: props.item,
    entity: 'KeyMeta',
  })?.relations?.['key'].data['name'];

  const oldTags = (getOnlyModifiedEntity({
    item: props.item,
    entity: 'KeyMeta',
  })?.modifications?.['tags']?.old || []) as any as string[];

  const newTags = (getOnlyModifiedEntity({
    item: props.item,
    entity: 'KeyMeta',
  })?.modifications?.['tags']?.new || []) as any as string[];

  const addedKeys = newTags.filter((newTag) => oldTags.indexOf(newTag) === -1);
  const removedTags = oldTags.filter(
    (oldTag) => newTags.indexOf(oldTag) === -1
  );

  return (
    <>
      <Box>
        <T
          parameters={{
            keyName: prepareValue(keyName),
            h: <ActivityValue />,
          }}
        >
          activity_tags_edit_for_key
        </T>
      </Box>
      <Box>
        {addedKeys.map((tag) => (
          <Box key={tag}>
            <T
              parameters={{
                tagName: prepareValue(tag),
                h: <ActivityValue />,
              }}
            >
              activity_tags_edit_added_tag
            </T>
          </Box>
        ))}
        {removedTags.map((tag) => (
          <Box key={tag}>
            <T
              parameters={{
                tagName: prepareValue(tag),
                h: <ActivityValue />,
              }}
            >
              activity_tags_edit_removed_tag
            </T>
          </Box>
        ))}
      </Box>
    </>
  );
};
