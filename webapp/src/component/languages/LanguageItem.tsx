import { Box } from '@mui/material';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';
import { components } from 'tg.service/apiSchema.generated';

type LanguageModel = components['schemas']['LanguageModel'];

type Props = {
  language: LanguageModel;
};

export const LanguageItem: React.FC<Props> = ({ language }) => {
  return (
    <Box display="inline-flex" alignItems="center" mr={2}>
      <Box mr={0.5} display="inline-flex" justifyContent="center">
        <CircledLanguageIcon size={18} flag={language.flagEmoji} />
      </Box>
      {`${language.name}${
        language.name !== language.originalName
          ? ` | ${language.originalName}`
          : ''
      } (${language.tag})`}
    </Box>
  );
};
