import * as React from 'react';
import { FC } from 'react';
import { components } from '../../service/apiSchema.generated';
import { Box } from '@material-ui/core';
import { FlagImage } from './FlagImage';

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
