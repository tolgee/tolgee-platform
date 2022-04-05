import { styled } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { getProviderImg } from './getProviderImg';
import { useTranslationTools } from './useTranslationTools';

type SuggestResultModel = components['schemas']['SuggestResultModel'];

const StyledContainer = styled('div')`
  display: flex;
  flex-direction: column;
`;

const StyledItem = styled('div')`
  padding: ${({ theme }) => theme.spacing(1, 1.25)};
  display: grid;
  gap: ${({ theme }) => theme.spacing(0, 1)};
  grid-template-columns: 20px 1fr;
  cursor: pointer;
  transition: all 0.1s ease-in-out;
  transition-property: background color;
  &:hover {
    background: ${({ theme }) => theme.palette.extraLightBackground.main};
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
};

export const MachineTranslation: React.FC<Props> = ({
  data,
  operationsRef,
}) => {
  const items = data?.machineTranslations
    ? Object.entries(data?.machineTranslations)
    : [];

  return (
    <StyledContainer>
      {items?.map(([provider, translation]) => {
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
            <StyledValue>{translation}</StyledValue>
          </StyledItem>
        );
      })}
    </StyledContainer>
  );
};
