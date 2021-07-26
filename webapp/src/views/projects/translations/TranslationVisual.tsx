import React, { useMemo } from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/core';

import { icuVariants } from 'tg.component/editor/icuVariants';
import { LimitedHeightText } from './LimitedHeightText';

const gradient = (color) =>
  `linear-gradient(to top, rgba(0,0,0,0) 0%, ${color} 1.2rem, ${color} 100%)`;

const useStyles = makeStyles({
  container: {},
  row: {
    display: 'flex',
    overflow: 'hidden',
  },
  wrapped: {
    overflow: 'hidden',
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis',
  },
  chipWrapper: {
    display: 'flex',
    alignItems: 'flex-start',
    width: 80,
    flexShrink: 0,
    overflow: 'hidden',
    position: 'relative',
    margin: '0px 8px 1px 0px',
  },
  chip: {
    display: 'inline',
    flexGrow: 0,
    padding: '0px 5px 1px 5px',
    background: 'lightgrey',
    borderRadius: 5,
    overflow: 'hidden',
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis',
  },
  fadeItem: {
    '& > *': {
      WebkitMaskImage: gradient('black'),
      maskImage: gradient('black'),
    },
  },
});

type Props = {
  maxLines?: number;
  wrapVariants?: boolean;
  text: string | undefined;
  locale: string;
};

export const TranslationVisual: React.FC<Props> = ({
  text,
  maxLines,
  wrapVariants,
  locale,
}) => {
  const classes = useStyles();
  const { variants } = useMemo(() => icuVariants(text || '', locale), [text]);

  if (!variants) {
    return <LimitedHeightText maxLines={maxLines}>{text}</LimitedHeightText>;
  } else if (variants.length === 1) {
    return (
      <LimitedHeightText maxLines={maxLines}>
        {variants[0].value}
      </LimitedHeightText>
    );
  } else {
    const croppedVariants = variants.slice(0, maxLines);
    const isOverflow = croppedVariants.length !== variants.length;
    return (
      <>
        {croppedVariants.map(({ option, value }, i) => (
          <div
            key={i}
            className={clsx(
              classes.row,
              i === croppedVariants.length - 1 && isOverflow
                ? classes.fadeItem
                : undefined
            )}
          >
            <div className={classes.chipWrapper}>
              <div className={classes.chip}>{option}</div>
            </div>
            <div className={wrapVariants ? classes.wrapped : undefined}>
              {value}
            </div>
          </div>
        ))}
      </>
    );
  }
};
