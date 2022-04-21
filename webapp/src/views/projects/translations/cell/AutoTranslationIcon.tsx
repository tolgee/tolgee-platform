import { Tooltip, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import {
  MachineTranslationIcon,
  TranslationMemoryIcon,
} from 'tg.component/CustomIcons';
import { getProviderImg } from '../TranslationTools/getProviderImg';

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
};

export const AutoTranslationIcon: React.FC<Props> = ({ provider }) => {
  const providerImg = getProviderImg(provider);
  const t = useTranslate();

  return (
    <Tooltip
      title={
        provider
          ? t('translations_auto_translated_provider', {
              provider: provider,
            })
          : t('translations_auto_translated_tm')
      }
    >
      {provider && providerImg ? (
        <StyledProviderImg src={providerImg} />
      ) : (
        <StyledImgWrapper>
          {provider ? (
            <MachineTranslationIcon className="icon" />
          ) : (
            <TranslationMemoryIcon className="icon" />
          )}
        </StyledImgWrapper>
      )}
    </Tooltip>
  );
};
