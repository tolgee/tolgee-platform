import { Button, Skeleton, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import clsx from 'clsx';

import { getLanguageDirection } from 'tg.fixtures/getLanguageDirection';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import { GoToBilling } from 'tg.component/GoToBilling';
import { stringHash } from 'tg.fixtures/stringHash';
import { ProviderLogo } from './ProviderLogo';
import { useMTStreamed } from './useMTStreamed';
import { TabMessage } from '../../common/TabMessage';
import { TranslationWithPlaceholders } from '../../../translationVisual/TranslationWithPlaceholders';
import { PanelContentProps } from '../../common/types';
import { useEffect } from 'react';

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

const OutOfCreditsWrapper = styled('div')`
  padding: 8px 12px 8px 12px;
  margin: 8px 8px 0px 8px;
  display: flex;
  flex-direction: column;
  position: relative;
  background: ${({ theme }) => theme.palette.cell.selected};
  border-radius: 8px;
`;

export const MachineTranslation: React.FC<PanelContentProps> = ({
  keyData,
  language,
  project,
  setValue,
  setItemsCount,
}) => {
  const { t } = useTranslate();

  const deps = {
    keyId: keyData.keyId,
    targetLanguageId: language.id,
  };

  const dependenciesHash = stringHash(JSON.stringify(deps));

  const machineLoadable = useMTStreamed({
    path: { projectId: project.id },
    // @ts-ignore add all dependencies to properly update query
    query: { hash: dependenciesHash },
    content: {
      'application/json': {
        ...deps,
      },
    },
    fetchOptions: {
      // error is displayed inside the popup
      disableAutoErrorHandle: false,
    },
    options: {
      keepPreviousData: true,
    },
  });

  const data = machineLoadable.data;

  const baseIsEmpty = data?.baseBlank;
  const nothingFetched = !data?.servicesTypes;
  const results = data?.servicesTypes.map(
    (provider) => [provider, data.result[provider]] as const
  );
  const arrayResults = Object.values(data?.result || {});
  const outOfCredit =
    arrayResults.every((i) => i?.errorMessage === 'OUT_OF_CREDITS') &&
    Boolean(arrayResults.length);
  const contextPresent = keyData.contextPresent;

  useEffect(() => {
    setItemsCount(arrayResults.length);
  }, [arrayResults.length]);

  return (
    <StyledContainer>
      {outOfCredit ? (
        <OutOfCreditsWrapper>
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
        </OutOfCreditsWrapper>
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
                  setValue(result!.output);
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
                    <div dir={getLanguageDirection(language.tag)}>
                      <TranslationWithPlaceholders
                        content={result.output || ''}
                        locale={language.tag}
                        nested={false}
                      />
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
              ) : !data && machineLoadable?.isFetching ? (
                <Skeleton variant="text" />
              ) : null}
            </StyledItem>
          );
        })
      )}
    </StyledContainer>
  );
};
