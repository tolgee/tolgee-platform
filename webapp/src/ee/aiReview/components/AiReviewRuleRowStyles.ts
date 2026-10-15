import { Link, styled } from '@mui/material';

export const StyledRuleDetails = styled('div')`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(0.5, 1)};
  font-size: 14px;
  color: ${({ theme }) => theme.palette.text.secondary};

  & .separator {
    opacity: 0.6;
  }
`;

export const StyledRuleLink = styled(Link)`
  cursor: pointer;
  font-size: 14px;
`;
