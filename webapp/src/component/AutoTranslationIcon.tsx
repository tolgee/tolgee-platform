import { Tooltip, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import {
  MachineTranslationIcon,
  TranslationMemoryIcon,
} from 'tg.component/CustomIcons';
import { useProviderImg } from 'tg.views/projects/translations/TranslationTools/useProviderImg';

const StyledImgWrapper = styled('div')`
  display: flex;
  & .icon {
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

const getContent = (
  provider: string | undefined,
  providerImg: string | null
) => {
  return (
    <StyledImgWrapper>
      {provider && providerImg ? (
        <StyledProviderImg src={providerImg} />
      ) : provider ? (
        <MachineTranslationIcon className="icon" />
      ) : (
        <TranslationMemoryIcon className="icon" />
      )}{' '}
    </StyledImgWrapper>
  );
};

export const AutoTranslationIcon: React.FC<Props> = ({
  provider,
  noTooltip,
}) => {
  const getProviderImg = useProviderImg();
  const providerImg = getProviderImg(provider);
  const t = useTranslate();

  return noTooltip ? (
    getContent(provider, providerImg)
  ) : (
    <Tooltip
      title={
        provider
          ? t('translations_auto_translated_provider', {
              provider: provider,
            })
          : t('translations_auto_translated_tm')
      }
    >
      {getContent(provider, providerImg)}
    </Tooltip>
  );
};
