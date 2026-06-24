import { ComponentProps, default as React, VFC } from 'react';
import { Box } from '@mui/material';

import { EmptyState } from 'tg.component/common/EmptyState';
import {
  AddFirstTranslationMemoryMessage,
  AddFirstTranslationMemoryMessageProps,
} from './AddFirstTranslationMemoryMessage';

type Props = {
  loading?: boolean;
  wrapperProps?: ComponentProps<typeof Box>;
} & AddFirstTranslationMemoryMessageProps;

export const TranslationMemoriesEmptyListMessage: VFC<Props> = ({
  loading,
  wrapperProps,
  ...otherProps
}) => {
  return (
    <EmptyState loading={loading} wrapperProps={wrapperProps}>
      <AddFirstTranslationMemoryMessage {...otherProps} />
    </EmptyState>
  );
};
