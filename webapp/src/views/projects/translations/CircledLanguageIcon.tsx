import React from 'react';
import { Box } from '@material-ui/core';
import { FlagImage } from 'tg.component/languages/FlagImage';

type Props = {
  flag: string | null | undefined;
};

export const CircledLanguageIcon: React.FC<Props> = ({ flag }) => {
  const size = 18;
  return (
    <Box
      borderRadius="50%"
      overflow="hidden"
      display="flex"
      justifyContent="center"
      alignItems="center"
      width={size}
      height={size}
      border="1px solid grey"
      flexShrink="0"
    >
      <FlagImage
        height={Math.floor(size * 0.7) * 2}
        width={Math.floor(size * 0.7) * 2}
        flagEmoji={flag || ''}
      />
    </Box>
  );
};
