import { Box, styled, Typography } from '@mui/material';
import { ChevronDown, ChevronUp } from '@untitled-ui/icons-react';

const StyledToggle = styled(Box)`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(0.5)};
  cursor: pointer;
  user-select: none;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledTitle = styled(Typography)`
  font-size: 14px;
  font-weight: 500;
  text-transform: uppercase;
`;

type Props = {
  title: string;
  expanded: boolean;
  onToggle: () => void;
  dataCy: string;
};

export const RuleSectionToggle = ({
  title,
  expanded,
  onToggle,
  dataCy,
}: Props) => (
  <StyledToggle onClick={onToggle} data-cy={dataCy}>
    <StyledTitle>{title}</StyledTitle>
    {expanded ? (
      <ChevronUp width={18} height={18} />
    ) : (
      <ChevronDown width={18} height={18} />
    )}
  </StyledToggle>
);
