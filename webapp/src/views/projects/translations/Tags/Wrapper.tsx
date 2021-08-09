import { makeStyles } from '@material-ui/core';
import clsx from 'clsx';

import { stopBubble } from 'tg.fixtures/eventHandler';

const useStyles = makeStyles((theme) => ({
  wrapper: {
    outline: 0,
    margin: 0,
    cursor: 'default',
    padding: '3px 7px',
    borderRadius: '12px',
    display: 'flex',
    alignItems: 'center',
    height: '22px',
    fontSize: 14,
    background: 'lightgrey',
    marginTop: 2,
    zIndex: 1,
    border: '1px solid transparent',
    maxWidth: '100%',
  },
  preview: {
    background: 'transparent',
    border: '1px solid lightgrey',
    color: theme.palette.text.secondary,
    '&:focus-within': {
      border: `1px solid ${theme.palette.primary.main}`,
      '-webkit-box-shadow': '0px 0px 2px 0px #000000',
      'box-shadow': '0px 0px 2px 0px #000000',
    },
  },
  clickable: {
    cursor: 'pointer',
  },
}));

type Props = {
  role?: 'input' | 'add';
  onClick?: () => void;
  className?: string;
};

export const Wrapper: React.FC<Props> = ({
  children,
  role,
  onClick,
  className,
}) => {
  const classes = useStyles();

  switch (role) {
    case 'add':
      return (
        <button
          data-cy="translations-tag-add"
          className={clsx(
            classes.wrapper,
            classes.preview,
            classes.clickable,
            className
          )}
          onClick={stopBubble(onClick)}
        >
          {children}
        </button>
      );
    case 'input':
      return (
        <div
          data-cy="translations-tag-input"
          className={clsx(classes.wrapper, classes.preview, className)}
          onClick={stopBubble()}
        >
          {children}
        </div>
      );
    default:
      return (
        <div
          data-cy="translations-tag"
          className={clsx(classes.wrapper, className)}
          onClick={stopBubble()}
        >
          {children}
        </div>
      );
  }
};
