import { Link, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';

type Props = {
  name: string;
  deleted?: boolean;
};

export const BranchNameLink = ({ name, deleted }: Props) => {
  const project = useProject();
  return !deleted ? (
    <Link
      component={RouterLink}
      to={LINKS.PROJECT_TRANSLATIONS_WITH_BRANCH.build({
        [PARAMS.PROJECT_ID]: project.id,
        [PARAMS.BRANCH]: name,
      })}
    >
      {name}
    </Link>
  ) : (
    <Typography variant="body2" color={(theme) => theme.palette.text.disabled}>
      {name}
    </Typography>
  );
};
