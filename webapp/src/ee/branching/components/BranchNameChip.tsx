import { Chip, styled } from '@mui/material';

const StyledChip = styled(Chip)`
  background-color: ${({ theme }) => theme.palette.tokens.secondary.disabled};
  border: 1px solid
    ${({ theme }) => theme.palette.tokens.secondary._states.outlinedBorder};
`;

type Props = {
  name: string;
};

export const BranchNameChip = ({ name }: Props) => {
  return <StyledChip size={'small'} label={name} />;
};
