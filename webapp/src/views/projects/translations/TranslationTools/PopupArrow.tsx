import { makeStyles } from '@material-ui/core';

const SIZE = 12;
const PADDING = 10;

const useStyles = makeStyles((theme) => ({
  wrapper: {
    top: -(SIZE + PADDING),
    padding: PADDING,
    paddingBottom: 0,
    position: 'absolute',
    overflowY: 'hidden',
    pointerEvents: 'none',
  },
  arrow: {
    width: 0,
    height: 0,
    borderLeft: `${SIZE}px solid transparent`,
    borderRight: `${SIZE}px solid transparent`,
    borderBottom: `${SIZE}px solid ${theme.palette.extraLightBackground.main}`,
    filter: `drop-shadow(0px 3px 5px rgba(0,0,0,0.5))`,
  },
}));

type Props = {
  position: string;
};

export const PopupArrow: React.FC<Props> = ({ position }) => {
  const classes = useStyles();

  return (
    <div
      className={classes.wrapper}
      style={{ left: `calc(${position} - ${PADDING + SIZE}px)` }}
    >
      <div className={classes.arrow} />
    </div>
  );
};
