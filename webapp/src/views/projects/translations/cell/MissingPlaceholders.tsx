import { styled, useTheme } from '@mui/material';
import {
  InvalidPlaceholder,
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

const StyledInvalid = styled('span')`
  margin-right: 8px;
  font-size: 14px;
  position: relative;
  bottom: 1px;
  color: ${({ theme }) => theme.palette.error.main};
`;

type Props = {
  placeholders: Placeholder[];
  invalidPlaceholders: InvalidPlaceholder[];
  onPlaceholderClick: (placeholder: Placeholder) => void;
  locale: string;
  variant: string | undefined;
  className?: string;
};

export const MissingPlaceholders = ({
  placeholders,
  invalidPlaceholders,
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
      {Boolean(invalidPlaceholders.length) && (
        <StyledInvalid>
          <T
            keyName="translations_invalid_icu_placeholders"
            defaultValue="Invalid ICU: {value}"
            params={{
              value: invalidPlaceholders.map((p) => p.value).join(', '),
            }}
          />
        </StyledInvalid>
      )}
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
