import { Tooltip, styled } from '@mui/material';
import { T } from '@tolgee/react';

const StyledStar = styled('span')`
  color: ${({ theme }) => theme.palette.primary.main};
`;

type Props = {
  children?: React.ReactNode;
};

export const RequiredField = ({ children }: Props) => {
  return (
    <Tooltip
      title={<T keyName="global_form_field_required" />}
      disableInteractive
    >
      <span>
        {children}
        <StyledStar>*</StyledStar>
      </span>
    </Tooltip>
  );
};
