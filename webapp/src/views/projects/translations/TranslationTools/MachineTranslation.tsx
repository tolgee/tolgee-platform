import { styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { TabMessage } from './TabMessage';
import { useTranslationTools } from './useTranslationTools';
import { getLanguageDirection } from 'tg.fixtures/getLanguageDirection';
import { ProviderLogo } from './ProviderLogo';

type SuggestResultModel = components['schemas']['SuggestResultModel'];

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
  cursor: pointer;
  transition: all 0.1s ease-in-out;
  transition-property: background color;

  &:hover {
    background: ${({ theme }) => theme.palette.emphasis[100]};
    color: ${({ theme }) => theme.palette.primary.main};
  }
`;

const StyledValue = styled('div')`
  font-size: 15px;
  align-self: center;
`;

const StyledDescription = styled('div')`
  font-size: 13px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type Props = {
  data: SuggestResultModel | undefined;
  operationsRef: ReturnType<typeof useTranslationTools>['operationsRef'];
  languageTag: string;
  contextPresent: boolean | undefined;
};

export const MachineTranslation: React.FC<Props> = ({
  data,
  operationsRef,
  languageTag,
  contextPresent,
}) => {
  const { t } = useTranslate();
  const baseIsEmpty = data?.machineTranslations === null;
  const items = data?.machineTranslations
    ? Object.entries(data?.machineTranslations)
    : [];

  return (
    <StyledContainer>
      {baseIsEmpty ? (
        <TabMessage>{t('translation_tools_base_empty')}</TabMessage>
      ) : (
        items?.map(([provider, translation]) => {
          return (
            <StyledItem
              key={provider}
              onMouseDown={(e) => {
                e.preventDefault();
              }}
              onClick={() => {
                operationsRef.current.updateTranslation(translation.output);
              }}
              role="button"
              data-cy="translation-tools-machine-translation-item"
            >
              <ProviderLogo
                provider={provider}
                contextPresent={contextPresent}
              />
              <StyledValue dir={getLanguageDirection(languageTag)}>
                <div>{translation.output}</div>
                {translation.contextDescription && (
                  <StyledDescription>
                    {translation.contextDescription}
                  </StyledDescription>
                )}
              </StyledValue>
            </StyledItem>
          );
        })
      )}
    </StyledContainer>
  );
};
