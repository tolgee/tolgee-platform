import { default as React, FunctionComponent } from 'react';
import { Box } from '@material-ui/core';
import { T } from '@tolgee/react';

import { SadEmotionMessage } from './SadEmotionMessage';

type Props = {
  hint?: React.ReactNode;
};

export const EmptyListMessage: FunctionComponent<Props> = (props) => {
  return (
    <Box p={8} data-cy="global-empty-list">
      <SadEmotionMessage hint={props.hint}>
        {props.children || <T>global_empty_list_message</T>}
      </SadEmotionMessage>
    </Box>
  );
};
