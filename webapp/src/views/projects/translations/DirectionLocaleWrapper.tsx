import { Box, BoxProps } from '@mui/material';
import React from 'react';
import { getLanguageDirection } from 'tg.fixtures/getLanguageDirection';

type Props = {
  languageTag: string;
} & BoxProps;

export const DirectionLocaleWrapper = React.forwardRef(
  function DirectionLocaleWrapper({ languageTag, ...props }: Props, ref) {
    return (
      <Box
        {...props}
        component="span"
        dir={languageTag ? getLanguageDirection(languageTag) : undefined}
        ref={ref}
      />
    );
  }
);
