import { ComponentProps, default as React, VFC } from 'react';
import { Box } from '@mui/material';

import {
  AddFirstGlossaryMessage,
  AddFirstGlossaryMessageProps,
} from './AddFirstGlossaryMessage';
import { EmptyState } from 'tg.component/common/EmptyState';

type Props = {
  loading?: boolean;
  wrapperProps?: ComponentProps<typeof Box>;
} & AddFirstGlossaryMessageProps;

export const GlossariesEmptyListMessage: VFC<Props> = ({
  loading,
  wrapperProps,
  ...otherProps
}) => {
  return (
    <EmptyState loading={loading} wrapperProps={wrapperProps}>
      <AddFirstGlossaryMessage {...otherProps} />
    </EmptyState>
  );
};
