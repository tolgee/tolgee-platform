import { styled, useTheme } from '@mui/material';
import {
  Placeholder,
  generatePlaceholdersStyle,
  getVariantExample,
} from '@tginternal/editor';
import { useMemo } from 'react';
import { placeholderToElement } from '../translationVisual/placeholderToElement';
import { T } from '@tolgee/react';

const StyledWrapper = styled('span')``;

const StyledLabel = styled('span')`
  margin-right: 8px;
  font-size: 14px;
  position: relative;
  bottom: 1px;
`;

type Props = {
  placeholders: Placeholder[];
  onPlaceholderClick: (placeholder: Placeholder) => void;
  locale: string;
  variant: string | undefined;
  className?: string;
};

export const MissingPlaceholders = ({
  placeholders,
  onPlaceholderClick,
  locale,
  variant,
  className,
}: Props) => {
  const theme = useTheme();
  const StyledPlaceholdersWrapper = useMemo(() => {
    return generatePlaceholdersStyle({
      styled,
      colors: theme.palette.placeholders,
      component: StyledWrapper,
    });
  }, [theme.palette.placeholders]);

  const pluralExampleValue = useMemo(() => {
    return getVariantExample(locale, variant ?? '');
  }, [locale, variant]);

  return (
    <StyledPlaceholdersWrapper
      className={className}
      onMouseDown={(e) => e.preventDefault()}
    >
      {Boolean(placeholders.length) && (
        <>
          <StyledLabel>
            <T keyName="translations_missing_placeholders_label" />
          </StyledLabel>
          {placeholders.map((value, i) =>
            placeholderToElement({
              placeholder: value,
              key: i,
              props: {
                onClick: () => onPlaceholderClick(value),
                style: { cursor: 'pointer' },
              },
              pluralExampleValue,
            })
          )}
        </>
      )}
    </StyledPlaceholdersWrapper>
  );
};
