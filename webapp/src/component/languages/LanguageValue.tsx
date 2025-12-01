import { FC } from 'react';
import { Box } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';

import { FlagImage } from '@tginternal/library/components/languages/FlagImage';

export const LanguageValue: FC<{
  language: Partial<components['schemas']['LanguageModel']>;
}> = (props) => {
  return (
    <Box display="inline-flex" justifyContent="center" justifyItems="center">
      {props.language.name}{' '}
      <Box ml={2}>
        <FlagImage width={20} flagEmoji={props.language.flagEmoji || ''} />
      </Box>
    </Box>
  );
};
