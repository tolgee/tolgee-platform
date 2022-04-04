import React from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { Box } from '@material-ui/core';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { SetTranslationsActivity } from './activities/translation/SetTranslationsActivity';
import { T } from '@tolgee/react';
import { KeyDeleteActivity } from './activities/key/KeyDeleteActivity';
import { TranslationCommentAddActivity } from './activities/translation/comment/TranslationCommentAddActivity';
import { TranslationCommentDeleteActivity } from './activities/translation/comment/TranslationCommentDeleteActivity';
import { TranslationCommentSetStateActivity } from './activities/translation/comment/TranslationCommentSetStateActivity';

export const ActivityItem = (props: {
  item: components['schemas']['ProjectActivityModel'];
}) => (
  <Box p={2} style={{ minHeight: 50, display: 'flex' }}>
    <Box display="flex">
      <Box>
        {props.item.author && (
          <AvatarImg
            owner={{ ...props.item.author, type: 'USER' }}
            size={40}
            autoAvatarType="IDENTICON"
            circle
          />
        )}
      </Box>
    </Box>

    <Box display="flex" flexDirection="column" ml={2}>
      <Box>{props.item.author?.name || <T>activity_unknown_author</T>}</Box>
      <Box>{new Date(props.item.timestamp).toLocaleString()}</Box>
    </Box>

    {activityComponents[props.item.type] ? (
      <Box ml={2}>
        <ActivityOfType item={props.item} />
      </Box>
    ) : (
      <>
        <Box ml={2}>
          <Box>{props.item.type}</Box>
        </Box>

        <Box ml={2}>
          <Box>{JSON.stringify(props.item.meta)}</Box>
        </Box>

        <Box ml={2}>
          <Box>{JSON.stringify(props.item.modifiedEntities)}</Box>
        </Box>
      </>
    )}
  </Box>
);

const ActivityOfType = (props: {
  item: components['schemas']['ProjectActivityModel'];
}) => {
  const ActivityComponent = activityComponents[props.item.type];

  return <ActivityComponent item={props.item} />;
};

const activityComponents = {
  SET_TRANSLATIONS: SetTranslationsActivity,
  KEY_DELETE_ACTIVITY: KeyDeleteActivity,
  TRANSLATION_COMMENT_ADD: TranslationCommentAddActivity,
  TRANSLATION_COMMENT_DELETE: TranslationCommentDeleteActivity,
  TRANSLATION_COMMENT_SET_STATE: TranslationCommentSetStateActivity,
};
