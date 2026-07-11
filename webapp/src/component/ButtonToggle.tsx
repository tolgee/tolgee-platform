import { Button, ButtonProps, styled } from '@mui/material';
import clsx from 'clsx';

const StyledButton = styled(Button)`
  margin-left: 8px;
  padding: 4px 8px;
  font-size: 13px;
  align-self: center;
  min-height: 0px !important;
  text-transform: none;
  font-style: normal;
  font-weight: 500;
  background-color: ${({ theme }) =>
    theme.palette.tokens._components.buttonToggle.enabled};
  color: ${({ theme }) =>
    theme.palette.tokens._components.buttonToggle.textEnabled};
  box-shadow: 0px 2px 8px 0px rgba(0, 0, 0, 0.2);
  border-radius: 4px;
  line-height: normal;

  :hover {
    background-color: ${({ theme }) =>
      theme.palette.tokens._components.buttonToggle.hovered};
    color: ${({ theme }) =>
      theme.palette.tokens._components.buttonToggle.textHovered};
    box-shadow: 0px 2px 8px 0px rgba(0, 0, 0, 0.2);
  }
  &.active {
    background-color: ${({ theme }) =>
      theme.palette.tokens._components.buttonToggle.active};
    color: ${({ theme }) =>
      theme.palette.tokens._components.buttonToggle.textActive};
    :hover {
      background-color: ${({ theme }) =>
        theme.palette.tokens._components.buttonToggle.activeHover};
      color: ${({ theme }) =>
        theme.palette.tokens._components.buttonToggle.textActiveHover};
    }
  }
`;

type Props = ButtonProps & {
  active?: boolean;
};

export const ButtonToggle = ({ active, className, ...other }: Props) => {
  return (
    <StyledButton
      variant="contained"
      disableElevation
      className={clsx({ active }, className)}
      {...other}
    />
  );
};
