import clsx from 'clsx';
import { styled } from '@mui/material';

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
  margin: ${({ theme }) => theme.spacing(0.5, 0.5)};
  border-radius: 4px;
  display: grid;
  gap: ${({ theme }) => theme.spacing(0, 1)};
  grid-template-columns: 20px 1fr;
  transition: all 0.1s ease-in-out;
  transition-property: background color;

  &:hover {
    background: ${({ theme }) => theme.palette.emphasis[50]};
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
};

export const MachineTranslationItem = ({
  data,
  provider,
  isFetching,
  contextPresent,
  languageTag,
  setValue,
  pluralVariant,
}: Props) => {
  const error = data?.errorMessage?.toLowerCase();
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
      data-cy="translation-tools-machine-translation-item"
      className={clsx({ clickable })}
    >
      <ProviderLogo provider={provider} contextPresent={contextPresent} />
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
          <TranslatedError code={error} />
        </StyledError>
      ) : !data && isFetching ? (
        <LoadingSkeletonFadingIn variant="text" />
      ) : null}
    </StyledItem>
  );
};
