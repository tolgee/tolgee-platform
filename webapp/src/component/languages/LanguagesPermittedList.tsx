import { T } from '@tolgee/react';
import { styled } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { CircledLanguageIcon } from './CircledLanguageIcon';
import clsx from 'clsx';

type LanguageModel = components['schemas']['LanguageModel'];

const ICON_SIZE = 20;

const StyledContainer = styled('div')`
  display: flex;
  flex-grow: 1;
  & .disabled {
    opacity: 0.2;
  }
`;

const StyledLabel = styled('div')`
  display: flex;
  flex-grow: 1;
  flex-shrink: 1;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  padding: 0px 5px;
`;

const StyledExtraCircle = styled('div')`
  box-sizing: border-box;
  width: ${ICON_SIZE}px;
  height: ${ICON_SIZE}px;
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
  maxItems?: number;
};

export const LanguagesPermittedList: React.FC<Props> = ({
  languages,
  disabled,
  maxItems = 3,
  ...props
}) => {
  const selectedLanguages = languages?.slice(0, maxItems) || [];

  const numOfExtra = (languages?.length || 0) - selectedLanguages.length;

  return !selectedLanguages.length ? (
    <StyledLabel>
      <T keyName="languages_permitted_list_all" />
    </StyledLabel>
  ) : (
    <StyledContainer {...props}>
      {selectedLanguages.map((l) => (
        <CircledLanguageIcon
          key={l.id}
          size={ICON_SIZE}
          flag={l.flagEmoji}
          className={clsx({
            disabled: Array.isArray(disabled)
              ? disabled.includes(l.id) || disabled.length === 0
              : disabled,
          })}
        />
      ))}
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
