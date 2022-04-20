import { ComponentProps, default as React, FunctionComponent } from 'react';
import { styled } from '@mui/material';
import ListItem from '@mui/material/ListItem';

const StyledListItem = styled(ListItem)`
  border-bottom: 1px solid ${({ theme }) => theme.palette.emphasis.A100};
  flex-wrap: wrap;
  &:last-child {
    border-bottom: none;
  }
`;

type PropTypes = Omit<ComponentProps<typeof ListItem>, 'button'> & {
  button?: boolean;
};

export const SimpleListItem: FunctionComponent<PropTypes> = (props) => {
  return (
    <StyledListItem
      data-cy="global-list-item"
      {...props}
      button={props.button as any}
    >
      {props.children}
    </StyledListItem>
  );
};
