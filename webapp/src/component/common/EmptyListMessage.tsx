import { ComponentProps, default as React, FunctionComponent } from 'react';
import { Box } from '@mui/material';
import { T } from '@tolgee/react';

import { SadEmotionMessage, SadEmotionMessageProps } from './SadEmotionMessage';
import { EmptyState } from './EmptyState';

type Props = {
  loading?: boolean;
  wrapperProps?: ComponentProps<typeof Box>;
} & SadEmotionMessageProps;

export const EmptyListMessage: FunctionComponent<Props> = ({
  loading,
  wrapperProps,
  children,
  ...otherProps
}) => {
  return (
    <EmptyState loading={loading} wrapperProps={wrapperProps}>
      <SadEmotionMessage {...otherProps}>
        {children || <T keyName="global_empty_list_message" />}
      </SadEmotionMessage>
    </EmptyState>
  );
};
