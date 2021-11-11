import { Box, makeStyles } from '@material-ui/core';
import { default as React, FC } from 'react';
import { guides } from 'tg.views/projects/integrate/guides';
import { ToggleButton } from '@material-ui/lab';
import { Guide } from 'tg.views/projects/integrate/types';

const useStyles = makeStyles((t) => ({
  root: {
    width: '100%',
    display: 'grid',
    gridTemplateColumns: 'repeat(auto-fill, 120px)',
    gap: 10,
    justifyContent: 'space-between',
  },
  weapon: {
    width: `100%`,
    cursor: 'pointer',
    '& > span': {
      flexDirection: 'column',
      flexGrow: 1,
      height: '100%',
    },
  },
  weaponName: {
    textAlign: 'center',
  },
  weaponIconWrapper: {
    marginBottom: 10,
    width: '100%',
    maxHeight: '60px',
    maxWidth: '60px',
    flexGrow: 1,
    lineHeight: 'initial',
  },
  weaponIcon: {
    width: '100%',
    height: '100%',
    objectFit: 'contain',
    fontSize: '50px',
  },
}));

export const WeaponSelector: FC<{
  selected: Guide | undefined;
  onSelect: (guide: Guide) => void;
}> = (props) => {
  const classes = useStyles();

  return (
    <Box className={classes.root}>
      {guides.map((g) => (
        <ToggleButton
          data-cy="integrate-weapon-selector-button"
          value={g.name}
          onClick={() => props.onSelect(g)}
          key={g.name}
          className={classes.weapon}
          selected={g === props.selected}
        >
          <Box className={classes.weaponIconWrapper}>
            {React.createElement(g.icon, { className: classes.weaponIcon })}
          </Box>
          <Box className={classes.weaponName}>{g.name}</Box>
        </ToggleButton>
      ))}
    </Box>
  );
};
