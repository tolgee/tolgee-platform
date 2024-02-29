import { useMemo } from 'react';
import { styled } from '@mui/material';
import React from 'react';
import {
  TolgeeFormat,
  getPluralVariants,
  getVariantExample,
} from '@tginternal/editor';

const StyledContainer = styled('div')`
  display: grid;
  gap: 2px;
`;

const StyledContainerSimple = styled('div')`
  padding-top: 4px;
`;

const StyledVariants = styled('div')`
  display: grid;
  grid-template-columns: 56px 1fr;
  gap: 8px;
`;

const StyledParameter = styled('div')`
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: 14px;
`;

const StyledVariantLabel = styled('div')`
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 24px;
  border: 1px solid ${({ theme }) => theme.palette.placeholders.variant.border};
  background-color: ${({ theme }) =>
    theme.palette.placeholders.variant.background};
  color: ${({ theme }) => theme.palette.placeholders.variant.text};
  border-radius: 12px;
  padding: 0px 9px;
  font-size: 14px;
  user-select: none;
  margin: 0px 1px;
  text-transform: capitalize;

  & > * {
    margin-top: -1px;
  }
`;

const StyledVariantContent = styled('div')`
  display: block;
`;

type RenderProps = {
  content: string;
  variant: string | undefined;
  locale: string;
  exampleValue?: number;
};

type Props = {
  locale: string;
  value: TolgeeFormat;
  render: (props: RenderProps) => React.ReactNode;
  showEmpty?: boolean;
  activeVariant?: string;
  variantPaddingTop?: number | string;
};

export const TranslationPlurals = ({
  locale,
  render,
  value,
  showEmpty,
  activeVariant,
  variantPaddingTop,
}: Props) => {
  const variants = useMemo(() => {
    const existing = new Set(Object.keys(value.variants));
    const required = getPluralVariants(locale);
    required.forEach((val) => existing.delete(val));
    const result = Array.from(existing).map((value) => {
      return [value, getVariantExample(locale, value)] as const;
    });
    required.forEach((value) => {
      result.push([value, getVariantExample(locale, value)]);
    });
    return result;
  }, [locale]);

  if (value.parameter) {
    return (
      <StyledContainer>
        <StyledParameter data-cy="translation-plural-parameter">
          {value.parameter}
        </StyledParameter>
        <StyledVariants>
          {variants
            .filter(([variant]) => showEmpty || value.variants[variant])
            .map(([variant, exampleValue]) => {
              const inactive = activeVariant && activeVariant !== variant;
              const opacity = inactive ? 0.5 : 1;
              return (
                <React.Fragment key={variant}>
                  <StyledVariantLabel
                    sx={{ opacity, marginTop: variantPaddingTop }}
                  >
                    <div>{variant}</div>
                  </StyledVariantLabel>
                  <StyledVariantContent
                    sx={{ opacity }}
                    data-cy="translation-plural-variant"
                  >
                    {render({
                      variant: variant,
                      content: value.variants[variant] || '',
                      exampleValue: exampleValue,
                      locale,
                    })}
                  </StyledVariantContent>
                </React.Fragment>
              );
            })}
        </StyledVariants>
      </StyledContainer>
    );
  }
  return (
    <StyledContainerSimple>
      {render({
        content: value.variants['other'] ?? '',
        locale,
        variant: undefined,
      })}
    </StyledContainerSimple>
  );
};
