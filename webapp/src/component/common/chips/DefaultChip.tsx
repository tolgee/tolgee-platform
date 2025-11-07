import { Chip, styled } from '@mui/material';

export const DefaultChip = styled(Chip)`
  padding: 0 ${({ theme }) => theme.spacing(1.5)};

  & .MuiChip-icon {
    margin-left: 0;
    margin-right: 4px;
    fill: ${({ theme }) => theme.palette.text.secondary};
  }

  & .MuiChip-label {
    padding-left: 0;
    padding-right: 0;
    font-size: 15px;
  }
`;
