import { DiffValue } from '../types';
import { styled } from '@mui/material';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';

const StyledContainer = styled('div')`
  display: flex;
  gap: 8px;
`;

const StyledLanguage = styled('div')`
  display: flex;
  align-items: center;
  gap: 4px;
  background: ${({ theme }) => theme.palette.emphasis[200]};
  border-radius: 4px;
  border: 1px solid ${({ theme }) => theme.palette.emphasis[300]};
  padding: 0px 4px;
`;

type Props = {
  input: DiffValue<number>;
};

const LanguageIdsComponent: React.FC<Props> = ({ input }) => {
  const allLangs = useProjectLanguages();
  const langId = input.new;
  const language = allLangs.find((lang) => lang.id === langId);
  return (
    <StyledContainer>
      <StyledLanguage key={langId}>
        {language?.name && <span>{language.name}</span>}
        <CircledLanguageIcon size={14} flag={language?.flagEmoji} />
      </StyledLanguage>
    </StyledContainer>
  );
};

export const getBatchLanguageIdChange = (input: DiffValue<number>) => {
  return <LanguageIdsComponent input={input} />;
};
