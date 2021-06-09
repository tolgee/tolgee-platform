import { FunctionComponent, useState } from 'react';
import { Box, Button, Popover } from '@material-ui/core';
import { getSvgNameByEmoji, supportedFlags } from '@tginternal/language-util';
import { useField } from 'formik';
import { makeStyles } from '@material-ui/core/styles';
import { ArrowDropDown } from '@material-ui/icons';

const getFlagPath = (hex: string) =>
  `/static/flags/${getSvgNameByEmoji(hex)}.svg`;

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
    margin: `0 ${theme.spacing(0.5)}`,
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
  const [field, meta, helpers] = useField(props.name);
  const [anchorEl, setAnchorEl] = useState<HTMLElement | null>(null);

  const svg = field.value || '🏁';
  const flags = [...new Set([...props.preferredEmojis, ...supportedFlags])];
  return (
    <>
      <Button
        className={classes.root}
        onClick={(event) => setAnchorEl(event.currentTarget)}
      >
        <img
          className={classes.selectedImg}
          loading="lazy"
          src={getFlagPath(svg)}
          alt={field.value}
        />

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
              <img
                loading="lazy"
                className={classes.selectorImg}
                src={getFlagPath(f)}
                alt={f}
              />
            </Button>
          ))}
        </Box>
      </Popover>
    </>
  );
};
