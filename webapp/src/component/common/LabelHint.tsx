import { HelpCircle } from '@untitled-ui/icons-react';
import { SxProps, Tooltip, styled } from '@mui/material';

const StyledLabelBody = styled('div')`
  display: inline-flex;
  gap: 4px;
  align-items: center;
`;

type Props = {
  size?: number;
  title: React.ReactNode;
  children: React.ReactNode;
  sx?: SxProps;
};

export const LabelHint = ({ children, title, size = 15, sx }: Props) => {
  return (
    <Tooltip title={title} disableInteractive>
      <StyledLabelBody {...{ sx }}>
        {children}
        <HelpCircle style={{ width: size, height: size }} />
      </StyledLabelBody>
    </Tooltip>
  );
};
