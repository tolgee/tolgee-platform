import { Link } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';

type Props = {
  name: string;
};

export const BranchNameLink = ({ name }: Props) => {
  const project = useProject();
  return (
    <Link
      component={RouterLink}
      to={LINKS.PROJECT_TRANSLATIONS_BRANCHED.build({
        [PARAMS.PROJECT_ID]: project.id,
        [PARAMS.TRANSLATIONS_BRANCH]: name,
      })}
    >
      {name}
    </Link>
  );
};
