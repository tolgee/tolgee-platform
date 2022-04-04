import React from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { T } from '@tolgee/react';
import { Box } from '@material-ui/core';
import {
  ActivityValue,
  getOnlyModifiedEntityModification,
  prepareValue,
} from '../../../activityUtil';

export const TranslationCommentAddActivity = (props: {
  item: components['schemas']['ProjectActivityModel'];
}) => {
  const commentText = getOnlyModifiedEntityModification({
    item: props.item,
    entity: 'TranslationComment',
    field: 'text',
  })?.new;

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
          activity_translation_comment_add
        </T>
      </Box>
    </>
  );
};
