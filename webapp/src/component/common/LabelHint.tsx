import { Help } from '@mui/icons-material';
import { Tooltip, styled } from '@mui/material';

const StyledLabelBody = styled('div')`
  display: inline-flex;
  gap: 4px;
  align-items: center;
`;

type Props = {
  size?: number;
  title: React.ReactNode;
  children: React.ReactNode;
};

export const LabelHint = ({ children, title, size = 15 }: Props) => {
  return (
    <Tooltip title={title} disableInteractive>
      <StyledLabelBody>
        {children}
        <Help style={{ fontSize: size }} />
      </StyledLabelBody>
    </Tooltip>
  );
};
