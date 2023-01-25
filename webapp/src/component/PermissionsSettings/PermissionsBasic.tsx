import { List, ListItemButton, ListItemText } from '@mui/material';
import { ProjectPermissionType } from 'tg.service/response.types';
import { PermissionBasic, PermissionModel } from './types';

type Props = {
  value: PermissionBasic;
  onChange: (value: PermissionBasic) => void;
};

export const PermissionsBasic: React.FC<Props> = ({ value, onChange }) => {
  const rolesList = Object.keys(ProjectPermissionType);

  return (
    <List>
      {rolesList.map((item) => {
        return (
          <ListItemButton
            key={item}
            selected={item === value.role}
            onClick={() => onChange({ role: item as PermissionModel['type'] })}
          >
            <ListItemText
              primary={item.toLowerCase()}
              secondary={<div>Hello</div>}
            />
          </ListItemButton>
        );
      })}
    </List>
  );
};
