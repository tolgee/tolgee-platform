import { styled } from '@mui/material';
import { T } from '@tolgee/react';

const StyledFormerUserName = styled('span')`
  opacity: 0.8;
`;

export const UserName = (props: {
  deleted?: boolean;
  username?: string;
  name?: string;
}) => {
  return props?.deleted === true ? (
    <StyledFormerUserName data-cy="former-user-name">
      <T>former-user-name</T>
    </StyledFormerUserName>
  ) : (
    <>{props?.name || props?.username}</>
  );
};
