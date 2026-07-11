import clsx from 'clsx';
import { styled, SxProps } from '@mui/material';

import { getLanguageDirection } from 'tg.fixtures/getLanguageDirection';
import { TranslatedError } from 'tg.translationTools/TranslatedError';

import { ProviderLogo } from './ProviderLogo';
import { TranslationWithPlaceholders } from '../../../translationVisual/TranslationWithPlaceholders';
import { CombinedMTResponse } from './useMTStreamed';
import {
  useExtractedPlural,
  useVariantExample,
} from '../../common/useExtractedPlural';
import { T } from '@tolgee/react';
import { LoadingSkeletonFadingIn } from 'tg.component/LoadingSkeleton';

const StyledItem = styled('div')`
  padding: ${({ theme }) => theme.spacing(0.5, 0.75)};
  margin: 4px 12px 4px 4px;
  border-radius: 4px;
  display: grid;
  gap: ${({ theme }) => theme.spacing(0, 1)};
  grid-template-columns: 20px 1fr;
  transition: all 0.1s ease-in-out;
  transition-property: background color;

  &:hover {
    background: ${({ theme }) => theme.palette.tokens.text._states.selected};
  }
  &.clickable {
    cursor: pointer;
    &:hover {
      color: ${({ theme }) => theme.palette.primary.main};
    }
  }
`;

const StyledValue = styled('div')`
  font-size: 15px;
  align-self: center;
  overflow-wrap: break-word;
  overflow: hidden;
`;

const StyledError = styled(StyledValue)`
  color: ${({ theme }) => theme.palette.error.main};
`;

const StyledDescription = styled('div')`
  font-size: 13px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledEmpty = styled(StyledValue)`
  font-style: italic;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type Props = {
  data: CombinedMTResponse['result'][number];
  provider: string;
  isFetching: boolean;
  contextPresent: boolean;
  setValue: (val: string) => void;
  languageTag: string;
  pluralVariant: string | undefined;
  showIcon?: boolean;
  sx?: SxProps;
  'data-cy'?: string;
};

export const MachineTranslationItem = ({
  data,
  provider,
  isFetching,
  contextPresent,
  languageTag,
  setValue,
  pluralVariant,
  showIcon = true,
  sx,
  'data-cy': dataCy,
}: Props) => {
  const error = data?.errorMessage?.toLowerCase();
  const errorParams = data?.errorParams;
  const result = data?.result;

  const text = useExtractedPlural(pluralVariant, data?.result?.output);
  const variantExample = useVariantExample(pluralVariant, languageTag);

  const clickable = Boolean(text);

  return (
    <StyledItem
      key={provider}
      onMouseDown={(e) => {
        if (clickable) {
          e.preventDefault();
        }
      }}
      onClick={() => {
        if (clickable) {
          setValue(text);
        }
      }}
      data-cy={dataCy}
      className={clsx({ clickable })}
      sx={{ gridTemplateColumns: showIcon ? '20px 1fr' : '1fr', ...sx }}
    >
      {showIcon && (
        <ProviderLogo provider={provider} contextPresent={contextPresent} />
      )}
      {result?.output ? (
        <>
          <StyledValue>
            <div dir={getLanguageDirection(languageTag)}>
              {text === '' ? (
                <StyledEmpty>
                  <T keyName="machine_translation_empty" />
                </StyledEmpty>
              ) : (
                <TranslationWithPlaceholders
                  content={text}
                  locale={languageTag}
                  nested={Boolean(pluralVariant)}
                  pluralExampleValue={variantExample}
                />
              )}
            </div>
            {result?.contextDescription && (
              <StyledDescription>{result.contextDescription}</StyledDescription>
            )}
          </StyledValue>
        </>
      ) : error ? (
        <StyledError>
          <TranslatedError code={error} params={errorParams} />
        </StyledError>
      ) : !data && isFetching ? (
        <StyledValue>
          <LoadingSkeletonFadingIn variant="text" />
          {provider === 'PROMPT' && (
            <StyledDescription>
              <LoadingSkeletonFadingIn variant="text" sx={{ width: '50%' }} />
            </StyledDescription>
          )}
        </StyledValue>
      ) : null}
    </StyledItem>
  );
};
