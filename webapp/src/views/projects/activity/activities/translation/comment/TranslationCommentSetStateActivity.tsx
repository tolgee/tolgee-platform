import React from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { T, useTranslate } from '@tolgee/react';
import { Box } from '@material-ui/core';
import {
  ActivityValue,
  getOnlyModifiedEntity,
  prepareValue,
} from '../../../activityUtil';

export const TranslationCommentSetStateActivity = (props: {
  item: components['schemas']['ProjectActivityModel'];
}) => {
  const modifiedComment = getOnlyModifiedEntity({
    item: props.item,
    entity: 'TranslationComment',
  });

  const newState = modifiedComment?.modifications?.['state']?.new as any;
  const commentText = modifiedComment?.description?.['text'];

  const t = useTranslate();

  const translationText = props.item.meta?.['translationText'];

  const stateTranslations = {
    NEEDS_RESOLUTION: t('translations_comments_needs_resolution'),
    RESOLVED: t('translations_comments_resolved'),
  };

  return (
    <>
      <Box>
        <T
          parameters={{
            translationText: prepareValue(translationText),
            commentText: prepareValue(commentText),
            newState: prepareValue(stateTranslations[newState || '']),
            h: <ActivityValue />,
          }}
        >
          activity_translation_comment_set_state
        </T>
      </Box>
    </>
  );
};
