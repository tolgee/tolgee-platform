import { FC } from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { useUser } from 'tg.globalContext/helpers';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { confirmation } from 'tg.hooks/confirmation';
import { T } from '@tolgee/react';
import { MenuItem, Select } from '@mui/material';

type Role = components['schemas']['UserAccountModel']['globalServerRole'];
type User = components['schemas']['UserAccountModel'];

export const RoleSelector: FC<{
  onSuccess: () => void;
  user: User;
}> = ({ user, onSuccess }) => {
  const currentUser = useUser();

  const setRoleMutation = useApiMutation({
    url: '/v2/administration/users/{userId}/set-role/{role}',
    method: 'put',
  });

  const message = useMessage();

  const setRole = (userId: number, role: Role) => {
    confirmation({
      onConfirm() {
        setRoleMutation.mutate(
          {
            path: {
              role: role,
              userId: userId,
            },
          },
          {
            onSuccess: () => {
              message.success(<T keyName="administration_role_set_success" />);
              onSuccess();
            },
          }
        );
      },
    });
  };

  return (
    <Select
      data-cy="administration-user-role-select"
      disabled={currentUser?.id === user.id}
      size="small"
      value={user.globalServerRole}
      onChange={(e) => setRole(user.id, e.target.value as Role)}
      inputProps={{ style: { padding: 0 } }}
    >
      <MenuItem value={'USER' satisfies Role}>
        <T keyName="administration_user_role_user" />
      </MenuItem>
      <MenuItem value={'SUPPORTER' satisfies Role}>
        <T keyName="administration_user_role_supporter" />
      </MenuItem>
      <MenuItem value={'ADMIN' satisfies Role}>
        <T keyName="administration_user_role_admin" />
      </MenuItem>
    </Select>
  );
};
