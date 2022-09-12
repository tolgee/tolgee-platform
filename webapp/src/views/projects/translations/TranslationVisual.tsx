import React, { useMemo } from 'react';
import { styled, Typography } from '@mui/material';

import { icuVariants } from 'tg.component/editor/icuVariants';
import { LimitedHeightText } from './LimitedHeightText';
import { DirectionLocaleWrapper } from './DirectionLocaleWrapper';

const StyledVariants = styled('div')`
  display: grid;
  grid-template-columns: 80px 1fr;
  column-gap: 4px;

  & .textWrapped {
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }

  & .chip {
    padding: 0px 5px 0px 5px;
    box-sizing: border-box;
    background: ${({ theme }) => theme.palette.emphasis[200]};
    border-radius: 4px;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
    max-width: 100%;
    justify-self: start;
    align-self: start;
    padding-bottom: 1px;
    height: 24px;
    margin-bottom: 2px;
  }
`;

const StyledParameter = styled(Typography)`
  display: flex;
  margin: 1px 0px 3px 0px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
`;

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
        <DirectionLocaleWrapper languageTag={locale}>
          {text}
        </DirectionLocaleWrapper>
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
          <StyledParameter variant="caption" color="textSecondary">
            {allParams}
          </StyledParameter>
        )}
        {variants.length === 1 ? (
          <LimitedHeightText
            width={width}
            maxLines={limitLines ? 3 : undefined}
            lang={locale}
          >
            <DirectionLocaleWrapper languageTag={locale}>
              {variants[0].value}
            </DirectionLocaleWrapper>
          </LimitedHeightText>
        ) : (
          <LimitedHeightText
            width={width}
            maxLines={limitLines ? 6 : undefined}
            lang={locale}
            lineHeight="26px"
          >
            <StyledVariants>
              {variants.map(({ option, value }, i) => (
                <React.Fragment key={i}>
                  <div className="chip">{option}</div>
                  <div className={limitLines ? 'textWrapped' : undefined}>
                    <DirectionLocaleWrapper languageTag={locale}>
                      {value}
                    </DirectionLocaleWrapper>
                  </div>
                </React.Fragment>
              ))}
            </StyledVariants>
          </LimitedHeightText>
        )}
      </div>
    );
  }
};
