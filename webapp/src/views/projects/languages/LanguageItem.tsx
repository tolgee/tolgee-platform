import { Box } from '@mui/material';

import { FlagImage } from 'tg.component/languages/FlagImage';
import { components } from 'tg.service/apiSchema.generated';

type LanguageModel = components['schemas']['LanguageModel'];

type Props = {
  language: LanguageModel;
};

export const LanguageItem: React.FC<Props> = ({ language }) => {
  return (
    <Box display="inline-flex" alignItems="center" mr={2}>
      <Box mr={1} display="inline-flex" justifyContent="center">
        <FlagImage width={20} flagEmoji={language.flagEmoji || '🏳️'} />
      </Box>
      {`${language.name}${
        language.name !== language.originalName
          ? ` | ${language.originalName}`
          : ''
      } (${language.tag})`}
    </Box>
  );
};
