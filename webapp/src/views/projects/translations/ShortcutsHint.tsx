import { useState } from 'react';
import clsx from 'clsx';
import { makeStyles, Typography } from '@material-ui/core';
import { Close, Help } from '@material-ui/icons';
import { useHideShortcuts } from 'tg.hooks/useHideShortcuts';
import { stopBubble } from 'tg.fixtures/eventHandler';

const useStyles = makeStyles((theme) => ({
  '@keyframes easeIn': {
    '0%': {
      opacity: 0,
    },
  },
  container: {
    display: 'flex',
    position: 'absolute',
    justifyContent: 'flex-end',
    right: 0,
    bottom: '100%',
    width: '100%',
    padding: theme.spacing(0, 1.5),
    transition: 'all 100ms ease-in-out',
  },
  containerCollapsed: {
    justifyContent: 'center',
    width: 60,
    bottom: 'calc(50% - 8px)',
  },
  content: {
    display: 'flex',
    justifyContent: 'space-between',
    background: theme.palette.lightBackground.main,
    borderRadius: 4,
    padding: '0px 6px',
    opacity: 0.5,
    '&:hover': {
      opacity: 1,
    },
    '&:hover $hoverHidden': {
      opacity: 1,
    },
    cursor: 'pointer',
    minHeight: 20,
    flexGrow: 1,
    transition: 'all 100ms ease-in-out',
  },
  contentExpanded: {
    opacity: 1,
    zIndex: theme.zIndex.tooltip,
  },
  contentCollapsed: {
    background: 'transparent',
  },
  items: {
    display: 'flex',
    flexDirection: 'column',
  },
  item: {
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    animationName: '$easeIn',
    animationDuration: '0.5s',
    animationTimingFunction: 'ease-in',
    opacity: 1,
    '& > * + *': {
      marginLeft: '0.4em',
    },
  },
  icon: {
    fontSize: 20,
  },
  hoverHidden: {
    opacity: 0,
    transition: 'opacity 300ms ease-in-out',
  },
}));

type Shortcut = {
  name: React.ReactNode;
  meta?: string;
  key: string;
};

type Props = {
  items: Shortcut[];
};

export const ShortcutsHint: React.FC<Props> = ({ items }) => {
  const classes = useStyles();
  const [collapsed, setCollapsed] = useHideShortcuts();
  const [expanded, setExpanded] = useState(false);

  const toggleExpand = () => setExpanded((val) => !val);
  const toggleCollapse = () => {
    setExpanded(false);
    setCollapsed(!collapsed);
  };

  return (
    <div
      className={clsx(
        classes.container,
        collapsed ? classes.containerCollapsed : undefined
      )}
    >
      <div
        className={clsx(
          classes.content,
          collapsed ? classes.contentCollapsed : undefined,
          expanded ? classes.contentExpanded : undefined
        )}
        onClick={collapsed ? toggleCollapse : toggleExpand}
      >
        <div className={classes.items}>
          {!collapsed &&
            items.slice(0, expanded ? items.length : 1).map((item, i) => {
              const metaString = item.meta ? `${item.meta} + ` : '';
              return (
                <div className={classes.item} key={i}>
                  <Typography variant="caption">{item.name}</Typography>
                  <Typography variant="caption" color="primary">
                    {`${metaString}${item.key}`}
                  </Typography>
                  {!expanded && items.length > 1 && (
                    <Typography variant="caption">...</Typography>
                  )}
                </div>
              );
            })}
        </div>
        {collapsed ? (
          <Help className={classes.icon} onClick={stopBubble(toggleCollapse)} />
        ) : (
          <Close
            className={clsx(classes.icon, classes.hoverHidden)}
            onClick={stopBubble(toggleCollapse)}
          />
        )}
      </div>
    </div>
  );
};
