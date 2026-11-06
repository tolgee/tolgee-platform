import { ReactNode } from 'react';
import { Box, Collapse, styled, Typography } from '@mui/material';
import { ChevronDown, ChevronRight } from '@untitled-ui/icons-react';

const StyledHeader = styled('button')`
  display: grid;
  grid-template-columns: 110px 1fr auto;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1.5)};
  width: 100%;
  padding: ${({ theme }) => theme.spacing(1.5, 1.75)};
  border: none;
  background: transparent;
  cursor: pointer;
  text-align: left;
  font: inherit;
  color: ${({ theme }) => theme.palette.text.primary};

  &:hover {
    background: ${({ theme }) => theme.palette.action.hover};
  }
`;

const StyledLabel = styled(Typography)`
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: 13px;
`;

const StyledSecondary = styled(Typography)`
  color: ${({ theme }) => theme.palette.text.secondary};
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

type Props = {
  label: string;
  primary: string;
  secondaryLines: string[];
  expanded: boolean;
  onToggle: () => void;
  dataCy: string;
  children: ReactNode;
};

export const RuleSummarySection = ({
  label,
  primary,
  secondaryLines,
  expanded,
  onToggle,
  dataCy,
  children,
}: Props) => (
  <div>
    <StyledHeader type="button" onClick={onToggle} data-cy={dataCy}>
      <StyledLabel>{label}</StyledLabel>
      <Box minWidth={0}>
        <Typography>{primary}</Typography>
        {secondaryLines.map((line, index) => (
          <StyledSecondary key={index} variant="body2">
            {line}
          </StyledSecondary>
        ))}
      </Box>
      {expanded ? (
        <ChevronDown width={18} height={18} />
      ) : (
        <ChevronRight width={18} height={18} />
      )}
    </StyledHeader>
    <Collapse in={expanded}>
      <Box padding="4px 14px 16px">{children}</Box>
    </Collapse>
  </div>
);
