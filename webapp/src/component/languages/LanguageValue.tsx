import { FC } from 'react';
import { Box } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';

import { FlagImage } from './FlagImage';

export const LanguageValue: FC<{
  language: Partial<components['schemas']['LanguageModel']>;
}> = (props) => {
  return (
    <Box
      display="inline-flex"
      justifyContent="center"
      gap={2}
      justifyItems="center"
    >
      {props.language.name}{' '}
      <FlagImage width={20} flagEmoji={props.language.flagEmoji || ''} />
    </Box>
  );
};
