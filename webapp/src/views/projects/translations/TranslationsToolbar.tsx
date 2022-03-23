import clsx from 'clsx';
import { useEffect, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { IconButton, makeStyles, Tooltip } from '@material-ui/core';
import { KeyboardArrowUp } from '@material-ui/icons';
import { useDebouncedCallback } from 'use-debounce';

import { useTranslationsSelector } from './context/TranslationsContext';
import { TranslationsShortcuts } from './TranslationsShortcuts';
import { useTheme } from '@material-ui/styles';

const useStyles = makeStyles((theme) => ({
  container: {
    zIndex: theme.zIndex.drawer,
    position: 'fixed',
    display: 'flex',
    alignItems: 'stretch',
    justifyContent: 'space-between',
    bottom: 0,
    right: 0,
    pointerEvents: 'none',
  },
  shortcutsContainer: {
    flexGrow: 1,
    margin: theme.spacing(2, 1, 2, 3),
    flexShrink: 1,
    flexBasis: 1,
    position: 'relative',
  },
  counterContainer: {
    display: 'flex',
    background: theme.palette.extraLightBackground.main,
    alignItems: 'stretch',
    transition: 'opacity 0.3s ease-in-out',
    borderRadius: 6,
    '-webkit-box-shadow': '2px 2px 5px rgba(0, 0, 0, 0.25)',
    'box-shadow': '2px 2px 5px rgba(0, 0, 0, 0.25)',
    margin: theme.spacing(2, 3, 2, 0),
    flexShrink: 0,
    whiteSpace: 'nowrap',
    pointerEvents: 'all',
  },
  hidden: {
    opacity: '0',
    pointerEvents: 'none',
    width: 0,
    marginRight: theme.spacing(1),
  },
  divider: {
    borderRight: `1px solid ${theme.palette.divider}`,
  },
  index: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'flex-end',
    justifyContent: 'center',
    marginRight: theme.spacing(2),
    marginLeft: theme.spacing(),
  },
  button: {
    flexShrink: 0,
    width: 40,
    height: 40,
  },
  stretcher: {
    fontFamily: 'monospace',
    height: 0,
    overflow: 'hidden',
  },
}));

type Props = {
  width: number;
};

export const TranslationsToolbar: React.FC<Props> = ({ width }) => {
  const [index, setIndex] = useState(1);
  const theme = useTheme();
  const [toolbarVisible, setToolbarVisible] = useState(false);
  const classes = useStyles();
  const t = useTranslate();
  const totalCount = useTranslationsSelector((c) => c.translationsTotal || 0);
  const list = useTranslationsSelector((c) => c.reactList);
  const getVisibleRange = list?.getVisibleRange.bind(list);

  const handleScrollUp = () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const onScroll = useDebouncedCallback(
    () => {
      const [start, end] = getVisibleRange?.() || [0, 0];
      const fromBeginning = start;
      const toEnd = totalCount - 1 - end;
      const total = fromBeginning + toEnd || 1;
      const progress = (total - toEnd) / total;
      const newIndex = Math.round(progress * (totalCount - 1) + 1);
      setIndex(newIndex);
      setToolbarVisible(start > 0 && newIndex > 1);
    },
    100,
    { maxWait: 200 }
  );

  useEffect(() => {
    onScroll();
    window.addEventListener('scroll', onScroll);
    return () => window.removeEventListener('scroll', onScroll);
  }, [getVisibleRange]);

  const counterContent = `${index} / ${totalCount}`;

  return width ? (
    <div
      className={classes.container}
      // @ts-ignore
      style={{ width: width + theme.spacing(8) }}
    >
      <div className={classes.shortcutsContainer}>
        <TranslationsShortcuts />
      </div>
      <div
        className={clsx({
          [classes.counterContainer]: true,
          [classes.hidden]: !toolbarVisible,
        })}
      >
        <div className={classes.index}>
          <span data-cy="translations-toolbar-counter">{counterContent}</span>
          {/* stretch content by monospace font, so it's not jumping */}
          <div className={classes.stretcher}>{counterContent}</div>
        </div>
        <div className={classes.divider} />
        <Tooltip title={t('translations_toolbar_to_top')}>
          <IconButton
            data-cy="translations-toolbar-to-top"
            onClick={handleScrollUp}
            size="small"
            className={classes.button}
            aria-label={t('translations_toolbar_to_top')}
          >
            <KeyboardArrowUp />
          </IconButton>
        </Tooltip>
      </div>
    </div>
  ) : null;
};
