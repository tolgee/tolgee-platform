import React from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { Box } from '@material-ui/core';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { T, useCurrentLanguage } from '@tolgee/react';
import { activityComponents } from './activityComponents';

export const ActivityItem = (props: {
  item: components['schemas']['ProjectActivityModel'];
}) => {
  const getCurrentLang = useCurrentLanguage();

  return (
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
        <Box>
          {new Date(props.item.timestamp).toLocaleString(getCurrentLang())}
        </Box>
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
};

const ActivityOfType = (props: {
  item: components['schemas']['ProjectActivityModel'];
}) => {
  const ActivityComponent = activityComponents[props.item.type];

  return <ActivityComponent item={props.item} />;
};
