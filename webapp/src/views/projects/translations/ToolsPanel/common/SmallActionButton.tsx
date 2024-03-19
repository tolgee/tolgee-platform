import { styled } from '@mui/material';

type Props = React.DetailedHTMLProps<
  React.ButtonHTMLAttributes<HTMLButtonElement>,
  HTMLButtonElement
>;

const StyledButton = styled('button')`
  margin: 0px;
  outline: 0;
  font-size: 12px;
  padding: 0px;
  border: 0px;
  opacity: 0.6;
  background: transparent;
  cursor: pointer;
  &:hover,
  &:active {
    opacity: 1;
  }
`;

export const SmallActionButton: React.FC<Props> = ({
  children,
  className,
  ...tools
}) => {
  return (
    <StyledButton {...tools} className={className}>
      {children}
    </StyledButton>
  );
};
