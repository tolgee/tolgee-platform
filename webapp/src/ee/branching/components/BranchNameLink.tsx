import { Link, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import { useBranchLinks } from 'tg.component/branching/useBranchLinks';

type Props = {
  name: string;
  deleted?: boolean;
};

export const BranchNameLink = ({ name, deleted }: Props) => {
  const { buildLink } = useBranchLinks();
  return !deleted ? (
    <Link component={RouterLink} to={buildLink('translations', name)}>
      {name}
    </Link>
  ) : (
    <Typography variant="body2" color={(theme) => theme.palette.text.disabled}>
      {name}
    </Typography>
  );
};
