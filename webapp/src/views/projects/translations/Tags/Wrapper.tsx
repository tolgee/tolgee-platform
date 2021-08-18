import { makeStyles } from '@material-ui/core';
import clsx from 'clsx';

import { stopBubble } from 'tg.fixtures/eventHandler';

const useStyles = makeStyles((theme) => ({
  wrapper: {
    display: 'flex',
    outline: 0,
    cursor: 'default',
    padding: '4px 4px',
    borderRadius: '12px',
    alignItems: 'center',
    height: '24px',
    fontSize: 14,
    background: '#D3D3D3',
    border: '1px solid transparent',
    maxWidth: '100%',
  },
  preview: {
    background: 'white',
    border: `1px solid ${theme.palette.text.secondary}`,
    color: theme.palette.text.secondary,
    '&:focus-within, &:hover': {
      border: `1px solid ${theme.palette.primary.main}`,
      color: theme.palette.primary.main,
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
