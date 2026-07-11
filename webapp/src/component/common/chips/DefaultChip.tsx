import { Chip, styled } from '@mui/material';

export const BaseChip = styled(Chip)`
  padding: 0 ${({ theme }) => theme.spacing(1.5)};

  & .MuiChip-icon {
    margin-left: 0;
    margin-right: 4px;
  }

  & .MuiChip-label {
    padding-left: 0;
    padding-right: 0;
    font-size: 15px;
  }
`;

export const DefaultChip = styled(BaseChip)`
  & .MuiChip-icon {
    fill: ${({ theme }) => theme.palette.text.secondary};
  }
`;
