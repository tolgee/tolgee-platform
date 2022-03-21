import { Theme } from '@mui/material';
import makeStyles from '@mui/styles/makeStyles';

export const useTableStyles = makeStyles<Theme>((theme) => ({
  table: {
    display: 'grid',
    alignItems: 'center',
    border: `1px ${theme.palette.extraLightDivider.main} solid`,
    borderRadius: 4,
    overflow: 'hidden',
    position: 'relative',
  },
  topRow: {
    display: 'flex',
    background: theme.palette.extraLightBackground.main,
    alignSelf: 'stretch',
    fontSize: 13,
    minWidth: 60,
    height: 24,
    padding: theme.spacing(0, 1),
    alignItems: 'center',
  },
  firstCell: {
    paddingLeft: theme.spacing(2),
  },
  lastCell: {
    justifySelf: 'end',
    paddingRight: theme.spacing(2),
  },
  centered: {
    display: 'flex',
    justifySelf: 'stretch',
    justifyContent: 'center',
  },
  divider: {
    gridColumn: '1 / -1',
    background: theme.palette.lightBackground.main,
    height: 1,
  },
}));
