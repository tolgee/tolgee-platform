import { T } from '@tolgee/react';
import { styled } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { CircledLanguageIcon } from './CircledLanguageIcon';
import clsx from 'clsx';

type LanguageModel = components['schemas']['LanguageModel'];

const StyledContainer = styled('div')`
  display: flex;
  align-items: center;
  justify-content: center;
  & .disabled {
    opacity: 0.2;
  }
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
  disabled?: boolean | number[];
};

export const LanguagesPermittedList: React.FC<Props> = ({
  languages,
  disabled,
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
          <CircledLanguageIcon
            key={l.id}
            size={20}
            flag={l.flagEmoji}
            className={clsx({
              disabled: Array.isArray(disabled)
                ? disabled.includes(l.id) || disabled.length === 0
                : disabled,
            })}
          />
        ))
      )}
      {numOfExtra > 0 && (
        <StyledExtraCircle
          className={clsx({
            disabled: disabled === true,
          })}
        >
          +{numOfExtra}
        </StyledExtraCircle>
      )}
    </StyledContainer>
  );
};
