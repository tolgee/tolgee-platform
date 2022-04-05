import { FunctionComponent, useState } from 'react';
import { Button, Popover, styled } from '@mui/material';
import { ArrowDropDown } from '@mui/icons-material';
import { supportedFlags } from '@tginternal/language-util';
import { useField } from 'formik';

import { FlagImage } from './FlagImage';

const StyledButton = styled(Button)`
  cursor: pointer;
  display: flex;
  align-items: center;
`;

const StyledImage = styled(FlagImage)`
  width: 50px;
  height: 50px;
`;

const StyledSelector = styled('div')`
  width: 300px;
  height: 400px;
  display: flex;
  flex-wrap: wrap;
`;

const StyledFlagButton = styled(Button)`
  padding: ${({ theme }) => theme.spacing(0.5)};
  min-width: 0px;
  & > span {
    height: 29px;
  }
`;

const StyledFlagImage = styled(FlagImage)`
  width: 50px;
  height: 50px;
`;

export const FlagSelector: FunctionComponent<{
  preferredEmojis: string[];
  name: string;
}> = (props) => {
  const [field, _, helpers] = useField(props.name);
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);

  const selectedEmoji = field.value || '🏳️';
  const flags = [...new Set([...props.preferredEmojis, ...supportedFlags])];
  return (
    <>
      <StyledButton
        data-cy="languages-flag-selector-open-button"
        onClick={(event) => setAnchorEl(event.currentTarget)}
      >
        <StyledImage flagEmoji={selectedEmoji} />
        <ArrowDropDown />
      </StyledButton>
      <Popover
        open={!!anchorEl}
        onClose={() => setAnchorEl(null)}
        anchorEl={anchorEl}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'center',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'left',
        }}
      >
        <StyledSelector>
          {flags.map((f) => (
            <StyledFlagButton
              key={f}
              onClick={() => {
                helpers.setValue(f);
                setAnchorEl(null);
              }}
            >
              <StyledFlagImage flagEmoji={f} />
            </StyledFlagButton>
          ))}
        </StyledSelector>
      </Popover>
    </>
  );
};
