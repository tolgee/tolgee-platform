import { styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import {
  MachineTranslationIcon,
  TranslationMemoryIcon,
} from 'tg.component/CustomIcons';
import { useProviderImg } from 'tg.views/projects/translations/TranslationTools/useProviderImg';
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
  const getProviderImg = useProviderImg();
  const providerImg = getProviderImg(provider);
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
            <MachineTranslationIcon />
          ) : (
            <TranslationMemoryIcon />
          )}
        </StyledImgWrapper>
      }
    />
  );
};
