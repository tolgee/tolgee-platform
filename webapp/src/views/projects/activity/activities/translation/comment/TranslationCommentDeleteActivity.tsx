import React from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { T } from '@tolgee/react';
import { Box } from '@mui/material';
import {
  ActivityValue,
  getOnlyModifiedEntity,
  prepareValue,
} from '../../../activityUtil';

export const TranslationCommentDeleteActivity = (props: {
  item: components['schemas']['ProjectActivityModel'];
}) => {
  const commentText = getOnlyModifiedEntity({
    item: props.item,
    entity: 'TranslationComment',
  })?.modifications?.['text']?.old;

  const translationText = props.item.meta?.['translationText'];

  return (
    <>
      <Box>
        <T
          parameters={{
            commentText: prepareValue(commentText),
            translationText: prepareValue(translationText),
            h: <ActivityValue />,
          }}
        >
          activity_translation_comment_delete
        </T>
      </Box>
    </>
  );
};
