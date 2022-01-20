import React, { useMemo } from 'react';
import { Typography, makeStyles } from '@material-ui/core';

import { icuVariants } from 'tg.component/editor/icuVariants';
import { LimitedHeightText } from './LimitedHeightText';

const useStyles = makeStyles((theme) => ({
  variants: {
    display: 'grid',
    gridTemplateColumns: '80px 1fr',
    columnGap: 4,
  },
  wrapped: {
    overflow: 'hidden',
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis',
  },
  parameter: {
    display: 'flex',
    margin: '1px 0px 3px 0px',
    overflow: 'hidden',
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis',
  },
  chip: {
    padding: '0px 5px 0px 5px',
    boxSizing: 'border-box',
    background: theme.palette.lightBackground.main,
    borderRadius: 4,
    overflow: 'hidden',
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis',
    maxWidth: '100%',
    justifySelf: 'start',
    alignSelf: 'start',
    paddingBottom: 1,
    height: 24,
    marginBottom: 2,
  },
}));

type Props = {
  limitLines?: boolean;
  wrapVariants?: boolean;
  text: string | undefined;
  locale: string;
  width?: number | string;
};

export const TranslationVisual: React.FC<Props> = ({
  text,
  limitLines,
  locale,
  width,
}) => {
  const classes = useStyles();
  const { variants, parameters } = useMemo(
    () => icuVariants(text || '', locale),
    [text]
  );

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
  } else {
    const allParams = parameters
      .filter((p) => !['argument', 'tag'].includes(p.function || ''))
      .map((p) => p.name)
      .join(', ');
    return (
      <div>
        {allParams && (
          <Typography
            className={classes.parameter}
            variant="caption"
            color="textSecondary"
          >
            {allParams}
          </Typography>
        )}
        {variants.length === 1 ? (
          <LimitedHeightText
            width={width}
            maxLines={limitLines ? 3 : undefined}
            lang={locale}
          >
            {variants[0].value}
          </LimitedHeightText>
        ) : (
          <LimitedHeightText
            width={width}
            maxLines={limitLines ? 6 : undefined}
            lang={locale}
            lineHeight="26px"
          >
            <div className={classes.variants}>
              {variants.map(({ option, value }, i) => (
                <React.Fragment key={i}>
                  <div className={classes.chip}>{option}</div>
                  <div className={limitLines ? classes.wrapped : undefined}>
                    {value}
                  </div>
                </React.Fragment>
              ))}
            </div>
          </LimitedHeightText>
        )}
      </div>
    );
  }
};
