import React, { useMemo } from 'react';
import { makeStyles } from '@material-ui/core';

import { icuVariants } from 'tg.component/editor/icuVariants';
import { LimitedHeightText } from './LimitedHeightText';
import clsx from 'clsx';

const gradient = (color) =>
  `linear-gradient(to top, rgba(0,0,0,0) 0%, ${color} 1.2rem, ${color} 100%)`;

const useStyles = makeStyles({
  variants: {
    display: 'grid',
    gridTemplateColumns: '80px 1fr',
    gap: 2,
  },
  wrapped: {
    overflow: 'hidden',
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis',
  },
  chip: {
    padding: '0px 5px 1px 5px',
    background: 'lightgrey',
    borderRadius: 5,
    overflow: 'hidden',
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis',
    justifySelf: 'start',
  },
  cropped: {
    WebkitMaskImage: gradient('black'),
    maskImage: gradient('black'),
  },
});

type Props = {
  limitLines?: boolean;
  wrapVariants?: boolean;
  text: string | undefined;
  locale: string;
  width;
};

export const TranslationVisual: React.FC<Props> = ({
  text,
  limitLines,
  locale,
  width,
}) => {
  const classes = useStyles();
  const { variants } = useMemo(() => icuVariants(text || '', locale), [text]);

  if (!variants) {
    return (
      <LimitedHeightText
        width={width}
        maxLines={limitLines ? 3 : undefined}
        lang={locale}
      >
        {text}
      </LimitedHeightText>
    );
  } else if (variants.length === 1) {
    return (
      <LimitedHeightText
        width={width}
        maxLines={limitLines ? 3 : undefined}
        lang={locale}
      >
        {variants[0].value}
      </LimitedHeightText>
    );
  } else {
    const croppedVariants = variants.slice(
      0,
      // display first 6 should be enough for plurals
      limitLines ? 6 : undefined
    );
    const isOverflow = croppedVariants.length !== variants.length;
    return (
      <div
        className={clsx({
          [classes.variants]: true,
          [classes.cropped]: isOverflow,
        })}
      >
        {croppedVariants.map(({ option, value }, i) => (
          <React.Fragment key={i}>
            <div className={classes.chip}>{option}</div>
            <div className={limitLines ? classes.wrapped : undefined}>
              {value}
            </div>
          </React.Fragment>
        ))}
      </div>
    );
  }
};
