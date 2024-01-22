import { Button, Skeleton, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { TabMessage } from './TabMessage';
import { useTranslationTools } from './useTranslationTools';
import { getLanguageDirection } from 'tg.fixtures/getLanguageDirection';
import { ProviderLogo } from './ProviderLogo';
import { CombinedMTResponse } from './useMTStreamed';
import { UseQueryResult } from 'react-query';
import { ApiError } from 'tg.service/http/ApiError';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import clsx from 'clsx';
import { GoToBilling } from 'tg.component/GoToBilling';

const StyledContainer = styled('div')`
  display: flex;
  flex-direction: column;
`;

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
    background: ${({ theme }) => theme.palette.emphasis[100]};
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

type Props = {
  machine: UseQueryResult<CombinedMTResponse, ApiError> | undefined;
  operationsRef: ReturnType<typeof useTranslationTools>['operationsRef'];
  languageTag: string;
  contextPresent: boolean | undefined;
};

export const MachineTranslation: React.FC<Props> = ({
  machine,
  operationsRef,
  languageTag,
  contextPresent,
}) => {
  const { t } = useTranslate();
  const data = machine?.data;
  const baseIsEmpty = data?.baseBlank;
  const nothingFetched = !data?.servicesTypes;
  const results = data?.servicesTypes.map(
    (provider) => [provider, data.result[provider]] as const
  );
  const arrayResults = Object.values(data?.result || {});
  const outOfCredit =
    arrayResults.every((i) => i?.errorMessage === 'OUT_OF_CREDITS') &&
    Boolean(arrayResults.length);

  return (
    <StyledContainer>
      {outOfCredit ? (
        <TabMessage>
          <StyledError
            sx={{ display: 'grid', gap: 0.5, justifyItems: 'start' }}
          >
            <T keyName="out_of_credits" />
            <GoToBilling
              render={(linkProps) => (
                <Button size="small" variant="outlined" {...linkProps}>
                  {t('machine_translation_buy_more_credit')}
                </Button>
              )}
            />
          </StyledError>
        </TabMessage>
      ) : baseIsEmpty ? (
        <TabMessage>{t('translation_tools_base_empty')}</TabMessage>
      ) : (
        !nothingFetched &&
        results?.map(([provider, data]) => {
          const error = data?.errorMessage?.toLowerCase();
          const result = data?.result;
          const clickable = data?.result?.output;
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
                  operationsRef.current.updateTranslation(result!.output);
                }
              }}
              data-cy="translation-tools-machine-translation-item"
              className={clsx({ clickable })}
            >
              <ProviderLogo
                provider={provider}
                contextPresent={contextPresent}
              />
              {result?.output ? (
                <>
                  <StyledValue>
                    <div dir={getLanguageDirection(languageTag)}>
                      {result?.output}
                    </div>
                    {result?.contextDescription && (
                      <StyledDescription>
                        {result.contextDescription}
                      </StyledDescription>
                    )}
                  </StyledValue>
                </>
              ) : error ? (
                <StyledError>
                  <TranslatedError code={error} />
                </StyledError>
              ) : !data && machine?.isFetching ? (
                <Skeleton variant="text" />
              ) : null}
            </StyledItem>
          );
        })
      )}
    </StyledContainer>
  );
};
