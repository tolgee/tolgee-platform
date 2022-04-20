import { T } from '@tolgee/react';
import { styled } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { CircledLanguageIcon } from './CircledLanguageIcon';

type LanguageModel = components['schemas']['LanguageModel'];

const StyledContainer = styled('div')`
  display: flex;
  align-items: center;
  justify-content: center;
`;

const StyledExtraCircle = styled('div')`
  box-sizing: border-box;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 2px;
  background: ${({ theme }) => theme.palette.grey[400]};
  color: ${({ theme }) => theme.palette.common.black};
  border-radius: 50%;
  font-size: 10px;
`;

type Props = React.HTMLAttributes<HTMLDivElement> & {
  languages?: LanguageModel[];
};

export const LanguagesPermittedList: React.FC<Props> = ({
  languages,
  ...props
}) => {
  const selectedLanguages = languages?.slice(0, 3) || [];

  const numOfExtra = (languages?.length || 0) - selectedLanguages.length;

  return (
    <StyledContainer {...props}>
      {!selectedLanguages.length ? (
        <T keyName="languages_permitted_list_all" />
      ) : (
        selectedLanguages.map((l) => (
          <CircledLanguageIcon key={l.id} size={20} flag={l.flagEmoji} />
        ))
      )}
      {numOfExtra > 0 && <StyledExtraCircle>+{numOfExtra}</StyledExtraCircle>}
    </StyledContainer>
  );
};
