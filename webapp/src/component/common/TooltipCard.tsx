import { Card, styled } from '@mui/material';

export const TooltipCard = styled(Card)`
  margin-top: ${({ theme }) => theme.spacing(1)};
  padding: ${({ theme }) => theme.spacing(2)};
  border-radius: ${({ theme }) => theme.spacing(2)};
  min-width: 400px;
  max-width: 500px;
`;
