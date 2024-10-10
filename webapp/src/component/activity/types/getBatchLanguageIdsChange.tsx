import { DiffValue } from '../types';
import { styled } from '@mui/material';
import { useProjectLanguages } from 'tg.hooks/useProjectLanguages';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';

const StyledContainer = styled('span')`
  display: div;
`;

const StyledLanguage = styled('span')`
  gap: 4px;
  background: ${({ theme }) => theme.palette.emphasis[100]};
  border-radius: 4px;
  border: 1px solid ${({ theme }) => theme.palette.emphasis[200]};
  padding: 0px 4px;
  & + & {
    margin-left: 4px;
  }
`;

const StyledName = styled('span')`
  overflow: hidden;
  text-overflow: ellipsis;
`;

type Props = {
  input: DiffValue<number[]>;
};

const LanguageIdsComponent: React.FC<Props> = ({ input }) => {
  const allLangs = useProjectLanguages();
  const newInput = input.new;
  if (newInput) {
    return (
      <StyledContainer>
        {input.new?.map((langId) => {
          const language = allLangs.find((lang) => lang.id === langId);
          return (
            <StyledLanguage key={langId}>
              {language?.name && <StyledName>{language.name}</StyledName>}
              <CircledLanguageIcon
                size={14}
                flag={language?.flagEmoji}
                display="inline-block"
                position="relative"
                top="3px"
                marginLeft="2px"
              />
            </StyledLanguage>
          );
        })}
      </StyledContainer>
    );
  } else {
    return null;
  }
};

export const getBatchLanguageIdsChange = (input: DiffValue<number[]>) => {
  return <LanguageIdsComponent input={input} />;
};
