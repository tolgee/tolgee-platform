import { styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { TabMessage } from './TabMessage';
import { useProviderImg } from './useProviderImg';
import { useTranslationTools } from './useTranslationTools';
import { getLanguageDirection } from 'tg.fixtures/getLanguageDirection';

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

const StyledSource = styled('div')`
  margin-top: 3px;
`;

const StyledValue = styled('div')`
  font-size: 15px;
  align-self: center;
`;

type Props = {
  data: SuggestResultModel | undefined;
  operationsRef: ReturnType<typeof useTranslationTools>['operationsRef'];
  languageTag: string;
};

export const MachineTranslation: React.FC<Props> = ({
  data,
  operationsRef,
  languageTag,
}) => {
  const { t } = useTranslate();
  const getProviderImg = useProviderImg();
  const baseIsEmpty = data?.machineTranslations === null;
  const items = data?.machineTranslations
    ? Object.entries(data?.machineTranslations)
    : [];

  return (
    <StyledContainer>
      {baseIsEmpty ? (
        <TabMessage
          type="placeholder"
          message={t('translation_tools_base_empty')}
        />
      ) : (
        items?.map(([provider, translation]) => {
          const providerImg = getProviderImg(provider);

          return (
            <StyledItem
              key={provider}
              onMouseDown={(e) => {
                e.preventDefault();
              }}
              onClick={() => {
                operationsRef.current.updateTranslation(translation);
              }}
              role="button"
              data-cy="translation-tools-machine-translation-item"
            >
              <StyledSource>
                {providerImg && <img src={providerImg} width="16px" />}
              </StyledSource>
              <StyledValue>
                <span dir={getLanguageDirection(languageTag)}>
                  {translation}
                </span>
              </StyledValue>
            </StyledItem>
          );
        })
      )}
    </StyledContainer>
  );
};
