import { Box, styled } from '@mui/material';

export const PlanInfoArea = styled(Box)`
  /* grid-area: info; */
`;

export const PlanInfo = styled(PlanInfoArea)`
  display: grid;
  justify-content: space-between;
  grid-template-columns: 1fr 16px 1fr;
  padding-bottom: 8px;
  justify-items: center;
`;
