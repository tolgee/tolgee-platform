import React from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { T } from '@tolgee/react';
import { Box } from '@material-ui/core';
import {
  ActivityValue,
  getOnlyModifiedEntity,
  prepareValue,
} from '../../../activityUtil';

export const TranslationCommentAddActivity = (props: {
  item: components['schemas']['ProjectActivityModel'];
}) => {
  const commentText = getOnlyModifiedEntity({
    item: props.item,
    entity: 'TranslationComment',
  })?.modifications?.['text']?.new;

  const keyName = getOnlyModifiedEntity({
    item: props.item,
    entity: 'TranslationComment',
  })?.relations?.['translation']['relations']['key'].data.name;

  const translationText = props.item.meta?.['translationText'];

  return (
    <>
      <Box>
        <T
          parameters={{
            keyName: prepareValue(keyName),
            commentText: prepareValue(commentText),
            translationText: prepareValue(translationText),
            h: <ActivityValue />,
          }}
        >
          activity_translation_comment_add
        </T>
      </Box>
    </>
  );
};
