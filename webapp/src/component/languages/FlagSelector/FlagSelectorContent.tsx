import { Button, Tooltip, styled } from '@mui/material';
import { FlagSearchField } from './FlagSearchField';
import { useMemo, useState } from 'react';
import {
  FlagImage,
  FlagInfo,
} from '@tginternal/library/components/languages/FlagImage';

const StyledSelector = styled('div')`
  width: 300px;
  height: 400px;
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr 1fr;
  padding: 0px 4px;
  overflow: auto;
  align-content: start;
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

type Props = {
  flagsInfo: FlagInfo[];
  onChange: (value: string) => void;
  setAnchorEl: (el: HTMLElement | null) => void;
};

export const FlagSelectorContent = ({
  flagsInfo,
  onChange,
  setAnchorEl,
}: Props) => {
  const [search, setSearch] = useState('');

  const filteredFlags = useMemo(() => {
    const searchLower = search.toLowerCase();
    return flagsInfo.filter(
      ({ name, code }) =>
        name.toLowerCase().includes(searchLower) ||
        code.toLowerCase().includes(searchLower)
    );
  }, [flagsInfo, search]);

  return (
    <>
      <FlagSearchField onChange={setSearch} />
      <StyledSelector>
        {filteredFlags.map((f) => (
          <Tooltip
            key={f.code}
            title={`${f.name} (${f.code})`}
            enterDelay={1000}
            enterNextDelay={1000}
          >
            <StyledFlagButton
              onClick={() => {
                onChange(f.emoji);
                setAnchorEl(null);
              }}
            >
              <StyledFlagImage flagEmoji={f.emoji} />
            </StyledFlagButton>
          </Tooltip>
        ))}
      </StyledSelector>
    </>
  );
};
