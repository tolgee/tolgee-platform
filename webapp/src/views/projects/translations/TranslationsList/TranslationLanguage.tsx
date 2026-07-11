import { styled, useTheme } from '@mui/material';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';
import { components } from 'tg.service/apiSchema.generated';
import { TranslationFlags } from '../cell/TranslationFlags';

type LanguageModel = components['schemas']['LanguageModel'];
type KeyWithTranslationsModel =
  components['schemas']['KeyWithTranslationsModel'];

const StyledLanguage = styled('div')`
  display: flex;
  grid-area: language;
  gap: 8px;
  align-items: center;
  font-size: 14px;
`;

type Props = {
  language: LanguageModel;
  keyData: KeyWithTranslationsModel;
  inactive?: boolean;
  className: string;
};

export const TranslationLanguage = ({
  language,
  keyData,
  inactive,
  className,
}: Props) => {
  const theme = useTheme();
  return (
    <StyledLanguage className={className}>
      <FlagImage flagEmoji={language.flagEmoji!} height={16} />
      <div
        data-cy="translations-table-cell-language"
        style={{
          fontWeight: language.base ? 'bold' : 'normal',
          color: inactive
            ? theme.palette.text.secondary
            : theme.palette.text.primary,
        }}
      >
        {language.name}
      </div>
      <TranslationFlags keyData={keyData} lang={language.tag} />
    </StyledLanguage>
  );
};
