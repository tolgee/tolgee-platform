import { styled } from '@mui/material';
import Box from '@mui/material/Box';

export const GlossaryListStyledRowCell = styled(Box)`
  display: grid;
  padding: ${({ theme }) => theme.spacing(1.5, 1.5)};
  border-top: 1px solid ${({ theme }) => theme.palette.divider1};

  &.clickable {
    cursor: pointer;

    &:focus-within {
      background: ${({ theme }) => theme.palette.cell.selected};
    }

    &:hover {
      --cell-background: ${({ theme }) => theme.palette.cell.hover};
      background: ${({ theme }) => theme.palette.cell.hover};
      transition: background 0.1s ease-in;
    }
  }
`;
