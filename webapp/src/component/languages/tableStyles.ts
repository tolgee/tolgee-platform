import { styled } from '@mui/material';

export const TABLE_TOP_ROW = 'topRow';
export const TABLE_FIRST_CELL = 'firstCell';
export const TABLE_LAST_CELL = 'lastCell';
export const TABLE_CENTERED = 'centered';
export const TABLE_DIVIDER = 'divider';

export const StyledLanguageTable = styled('div')`
  display: grid;
  align-items: center;
  border: 1px ${({ theme }) => theme.palette.divider1} solid;
  border-radius: 4px;
  overflow: hidden;
  position: relative;

  & .${TABLE_TOP_ROW} {
    display: flex;
    background: ${({ theme }) => theme.palette.emphasis[50]};
    align-self: stretch;
    font-size: 13px;
    height: 24px;
    padding: ${({ theme }) => theme.spacing(0, 1)};
    align-items: center;
  }

  & .${TABLE_FIRST_CELL} {
    display: flex;
    align-items: center;
    min-height: 50px;
    grid-column-start: 1;
    padding-left: ${({ theme }) => theme.spacing(2)};
  }

  & .${TABLE_LAST_CELL} {
    justify-self: end;
    padding-right: ${({ theme }) => theme.spacing(1)};
  }

  & .${TABLE_CENTERED} {
    display: flex;
    justify-self: stretch;
    justify-content: center;
  }

  & .${TABLE_DIVIDER} {
    grid-column: 1 / -1;
    background: ${({ theme }) => theme.palette.divider1};
    height: 1px;
  }
`;
