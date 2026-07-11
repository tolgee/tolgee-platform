import { styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

export type SimpleUser = {
  deleted?: boolean;
  username?: string;
  name?: string;
  id?: number;
};

const StyledFormerUserName = styled('span')`
  opacity: 0.8;
`;

export const useUserName = () => {
  const { t } = useTranslate();
  return (user: SimpleUser) => {
    if (user.deleted) {
      return t('former-user-name');
    } else {
      return user?.name || user?.username;
    }
  };
};

export const UserName = (props: SimpleUser) => {
  const getUserName = useUserName();
  return props?.deleted === true ? (
    <StyledFormerUserName data-cy="former-user-name">
      <T keyName="former-user-name" />
    </StyledFormerUserName>
  ) : (
    <>{getUserName(props)}</>
  );
};
