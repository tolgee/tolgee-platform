import { components } from 'tg.service/apiSchema.generated';
import { Link } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';

type SimpleProjectModel = components['schemas']['SimpleProjectModel'];

type Props = {
  project: SimpleProjectModel;
} & React.ComponentProps<typeof Link>;

export const ProjectLink: React.FC<Props> = ({ project, ...props }) => {
  return (
    <Link
      component={RouterLink}
      to={LINKS.PROJECT.build({ [PARAMS.PROJECT_ID]: project.id })}
      {...props}
    >
      {project.name}
    </Link>
  );
};
