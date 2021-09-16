import clsx from 'clsx';
import { useEffect, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { IconButton, makeStyles, Tooltip } from '@material-ui/core';
import { KeyboardArrowUp } from '@material-ui/icons';
import { useContextSelector } from 'use-context-selector';
import { useDebouncedCallback } from 'use-debounce';

import { TranslationsContext } from './context/TranslationsContext';

const useStyles = makeStyles((theme) => ({
  container: {
    display: 'flex',
    position: 'fixed',
    bottom: theme.spacing(2),
    right: theme.spacing(3),
    background: theme.palette.extraLightBackground.main,
    alignItems: 'stretch',
    transition: 'opacity 0.3s ease-in-out',
    borderRadius: 6,
    '-webkit-box-shadow': '2px 2px 5px rgba(0, 0, 0, 0.25)',
    'box-shadow': '2px 2px 5px rgba(0, 0, 0, 0.25)',
  },
  hidden: {
    opacity: '0',
    pointerEvents: 'none',
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
  getVisibleRange: (() => number[]) | undefined;
};

export const TranslationsToolbar: React.FC<Props> = ({ getVisibleRange }) => {
  const [index, setIndex] = useState(1);
  const [toolbarVisible, setToolbarVisible] = useState(false);
  const classes = useStyles();
  const t = useTranslate();
  const totalCount = useContextSelector(
    TranslationsContext,
    (c) => c.translationsTotal || 0
  );

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
      setIndex(Math.round(progress * (totalCount - 1) + 1));
      setToolbarVisible(start > 0 && index > 1);
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

  return (
    <div
      className={clsx({
        [classes.container]: true,
        [classes.hidden]: !toolbarVisible,
      })}
    >
      <div className={classes.index}>
        <span data-cy="translations-toolbar-counter">{counterContent}</span>
        {/* stretch content by monospace font, so it's not jumping */}
        <div className={classes.stretcher}>{counterContent}</div>
      </div>
      <div className={classes.divider} />
      <Tooltip title={t('translations_toolbar_to_top', undefined, true)}>
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
  );
};
