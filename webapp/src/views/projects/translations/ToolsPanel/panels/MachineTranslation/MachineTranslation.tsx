import { Button, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { GoToBilling } from 'tg.component/GoToBilling';
import { stringHash } from 'tg.fixtures/stringHash';
import { useMTStreamed } from './useMTStreamed';
import { TabMessage } from '../../common/TabMessage';
import { PanelContentProps } from '../../common/types';
import { useEffect } from 'react';
import { MachineTranslationItem } from './MachineTranslationItem';

const StyledContainer = styled('div')`
  display: flex;
  flex-direction: column;
`;

const StyledValue = styled('div')`
  font-size: 15px;
  align-self: center;
`;

const StyledError = styled(StyledValue)`
  color: ${({ theme }) => theme.palette.error.main};
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
  activeVariant,
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
          return (
            <MachineTranslationItem
              key={provider}
              data={data}
              provider={provider}
              isFetching={machineLoadable.isFetching}
              languageTag={language.tag}
              setValue={setValue}
              contextPresent={contextPresent}
              pluralVariant={activeVariant}
            />
          );
        })
      )}
    </StyledContainer>
  );
};
