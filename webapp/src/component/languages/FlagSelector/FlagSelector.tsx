import { FunctionComponent, useMemo, useState } from 'react';
import { Button, Popover, styled } from '@mui/material';
import { supportedFlags } from '@tginternal/language-util';
import { useField } from 'formik';
import countryFlagEmoji from 'country-flag-emoji';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';
import { FlagInfo } from './types';
import { FlagSelectorContent } from './FlagSelectorContent';
import { ArrowDropDown } from 'tg.component/CustomIcons';

const FLAGS_INFO: FlagInfo[] = [
  { code: 'empty', emoji: '🏳️', name: 'No flag' },
  ...countryFlagEmoji.list,
];

const StyledButton = styled(Button)`
  cursor: pointer;
  display: flex;
  align-items: center;
`;

const StyledImage = styled(FlagImage)`
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
  const flagsInfo = useMemo(() => {
    const flags = [...new Set([...props.preferredEmojis, ...supportedFlags])];
    return flags
      .map((emoji) => FLAGS_INFO.find((f) => f.emoji === emoji))
      .filter(Boolean) as FlagInfo[];
  }, [props.preferredEmojis, supportedFlags]);

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
        onClose={() => {
          setAnchorEl(null);
        }}
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
        <FlagSelectorContent
          flagsInfo={flagsInfo}
          setAnchorEl={setAnchorEl}
          onChange={helpers.setValue}
        />
      </Popover>
    </>
  );
};
