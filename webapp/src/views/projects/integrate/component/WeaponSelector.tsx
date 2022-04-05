import { Box, styled } from '@mui/material';
import { default as React, FC } from 'react';
import { guides } from 'tg.views/projects/integrate/guides';
import { ToggleButton } from '@mui/material';
import { Guide } from 'tg.views/projects/integrate/types';

const StyledRoot = styled('div')`
  width: 100%;
  display: grid;
  grid-template-columns: repeat(auto-fill, 120px);
  gap: 10px;
  justify-content: space-between;
`;

const StyledToggleButton = styled(ToggleButton)`
  width: 100%;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  & > span {
    flex-direction: column;
    flex-grow: 1;
    height: 100%;
  }
`;

const StyledIconWrapper = styled('div')`
  margin-bottom: 10px;
  width: 100%;
  max-height: 60px;
  max-width: 60px;
  flex-grow: 1;
  line-height: initial;
  & .weaponIcon {
    width: 100%;
    height: 100%;
    object-fit: contain;
    font-size: 50px;
  }
`;

export const WeaponSelector: FC<{
  selected: Guide | undefined;
  onSelect: (guide: Guide) => void;
}> = (props) => {
  return (
    <StyledRoot>
      {guides.map((g) => (
        <StyledToggleButton
          data-cy="integrate-weapon-selector-button"
          value={g.name}
          onClick={() => props.onSelect(g)}
          key={g.name}
          selected={g === props.selected}
        >
          <StyledIconWrapper>
            {React.createElement(g.icon, { className: 'weaponIcon' })}
          </StyledIconWrapper>
          <Box>{g.name}</Box>
        </StyledToggleButton>
      ))}
    </StyledRoot>
  );
};
