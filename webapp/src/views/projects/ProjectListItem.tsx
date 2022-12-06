import { Button, Chip } from '@mui/material';
import ListItemSecondaryAction from '@mui/material/ListItemSecondaryAction';
import ListItemText from '@mui/material/ListItemText';
import { T } from '@tolgee/react';
import { Link } from 'react-router-dom';

import { ListItemLink } from 'tg.component/common/list/ListItemLink';
import { LINKS, PARAMS } from 'tg.constants/links';
import { components } from 'tg.service/apiSchema.generated';

const ProjectListItem = (p: components['schemas']['ProjectModel']) => {
  return (
    <ListItemLink
      key={p.id}
      to={LINKS.PROJECT_DASHBOARD.build({ [PARAMS.PROJECT_ID]: p.id })}
    >
      <ListItemText>
        {p.name}{' '}
        <Chip
          data-cy="project-list-owner"
          size="small"
          label={p.organizationOwner?.name}
        />
      </ListItemText>
      {p.computedPermission?.scopes?.includes('admin') && (
        <ListItemSecondaryAction>
          <Button
            data-cy="project-settings-button"
            component={Link}
            size="small"
            variant="outlined"
            to={LINKS.PROJECT_EDIT.build({ [PARAMS.PROJECT_ID]: p.id })}
          >
            <T>project_settings_button</T>
          </Button>
        </ListItemSecondaryAction>
      )}
    </ListItemLink>
  );
};

export default ProjectListItem;
