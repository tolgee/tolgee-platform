import { styled } from '@mui/material';
import { CompareArrows } from '@mui/icons-material';
import { ConnectionItem, UserInfo } from './ConnectionItem';

export const StyledContainer = styled('div')`
  display: flex;
  gap: 20px;
  align-items: center;

  ${({ theme }) => theme.breakpoints.down('sm')} {
    flex-direction: column;
  }
`;

type Props = {
  first: UserInfo;
  second: UserInfo;
};

export const Connection = ({ first, second }: Props) => {
  return (
    <StyledContainer>
      <ConnectionItem data={first} />
      <CompareArrows />
      <ConnectionItem data={second} />
    </StyledContainer>
  );
};
