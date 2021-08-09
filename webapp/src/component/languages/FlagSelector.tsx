import { FunctionComponent, useState } from 'react';
import { Box, Button, Popover } from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';
import { ArrowDropDown } from '@material-ui/icons';
import { supportedFlags } from '@tginternal/language-util';
import { useField } from 'formik';

import { FlagImage } from './FlagImage';

const useStyles = makeStyles((theme) => ({
  root: {
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
  },
  selectedImg: {
    width: '50px',
    height: '50px',
  },
  selector: {
    width: '300px',
    height: '400px',
    display: 'flex',
    flexWrap: 'wrap',
  },
  selectorImg: {
    width: '40px',
    height: '40px',
  },
  selectorImgButton: {
    padding: theme.spacing(0.5),
    minWidth: 0,
    '& > span': {
      height: '29px',
    },
  },
}));

export const FlagSelector: FunctionComponent<{
  preferredEmojis: string[];
  name: string;
}> = (props) => {
  const classes = useStyles();
  const [field, _, helpers] = useField(props.name);
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);

  const selectedEmoji = field.value || '🏳️';
  const flags = [...new Set([...props.preferredEmojis, ...supportedFlags])];
  return (
    <>
      <Button
        data-cy="languages-flag-selector-open-button"
        className={classes.root}
        onClick={(event) => setAnchorEl(event.currentTarget)}
      >
        <FlagImage flagEmoji={selectedEmoji} className={classes.selectedImg} />
        <ArrowDropDown />
      </Button>
      <Popover
        open={!!anchorEl}
        onClose={() => setAnchorEl(null)}
        anchorEl={anchorEl}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'center',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'left',
        }}
      >
        <Box className={classes.selector}>
          {flags.map((f) => (
            <Button
              key={f}
              className={classes.selectorImgButton}
              onClick={() => {
                helpers.setValue(f);
                setAnchorEl(null);
              }}
            >
              <FlagImage flagEmoji={f} className={classes.selectorImg} />
            </Button>
          ))}
        </Box>
      </Popover>
    </>
  );
};
