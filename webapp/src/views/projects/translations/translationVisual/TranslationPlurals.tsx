import React, { useMemo } from 'react';
import { styled } from '@mui/material';
import {
  getPluralVariants,
  getVariantExample,
  TolgeeFormat,
} from '@tginternal/editor';

const StyledContainer = styled('div')`
  display: grid;
  gap: 2px;
  grid-template-rows: auto 1fr;
`;

const StyledContainerSimple = styled('div')`
  padding-top: 4px;
  display: grid;
`;

const StyledVariants = styled('div')`
  display: grid;
  grid-template-columns: 56px 1fr;
  align-content: start;
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
  white-space: nowrap;
  & > * {
    margin-top: -1px;
  }
`;

const StyledVariantContent = styled('div')`
  display: block;
`;

type RenderProps = {
  content: string | undefined;
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
  exactForms?: number[];
};

export const TranslationPlurals = ({
  locale,
  render,
  value,
  showEmpty,
  activeVariant,
  variantPaddingTop,
  exactForms,
}: Props) => {
  const variants = useMemo(
    () => getForms(locale, value, exactForms),
    [locale, exactForms, value]
  );

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

function getForms(locale: string, value: TolgeeFormat, exactForms?: number[]) {
  const forms: Set<string> = new Set();
  getPluralVariants(locale).forEach((value) => forms.add(value));
  Object.keys(value.variants).forEach((value) => forms.add(value));
  (exactForms || [])
    .map((value) => `=${value.toString()}`)
    .forEach((value) => forms.add(value));

  const formsArray = sortExactForms(forms);

  return formsArray.map((value) => {
    return [value, getVariantExample(locale, value)] as const;
  });
}

function sortExactForms(forms: Set<string>) {
  return [...forms].sort((a, b) => {
    if (a.startsWith('=') && b.startsWith('=')) {
      return Number(a.substring(1)) - Number(b.substring(1));
    }
    return 0;
  });
}
