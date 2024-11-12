import { styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Mt, TranslationMemory } from 'tg.component/CustomIcons';
import { useServiceImg } from 'tg.views/projects/translations/ToolsPanel/panels/MachineTranslation/useServiceImg';
import { TranslationFlagIcon } from './TranslationFlagIcon';

const StyledImgWrapper = styled('div')`
  display: flex;
  & * {
    font-size: 16px;
    color: #249bad;
  }
`;

const StyledProviderImg = styled('img')`
  width: 14px;
  height: 14px;
`;

type Props = {
  provider?: string;
  noTooltip?: boolean;
};

export const AutoTranslationIcon: React.FC<Props> = ({
  provider,
  noTooltip,
}) => {
  const getProviderImg = useServiceImg();
  const providerImg = getProviderImg(provider, false);
  const { t } = useTranslate();

  return (
    <TranslationFlagIcon
      tooltip={
        !noTooltip &&
        (provider
          ? t('translations_auto_translated_provider', {
              provider: provider,
            })
          : t('translations_auto_translated_tm'))
      }
      icon={
        <StyledImgWrapper>
          {provider && providerImg ? (
            <StyledProviderImg src={providerImg} />
          ) : provider ? (
            <Mt />
          ) : (
            <TranslationMemory />
          )}
        </StyledImgWrapper>
      }
    />
  );
};
