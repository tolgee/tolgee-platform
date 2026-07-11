import { styled, Tooltip } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useServiceImg } from '../../../../../../hooks/useServiceImg';

const StyledSource = styled('div')`
  margin-top: 3px;
`;

type Props = {
  provider: string | undefined;
  contextPresent: boolean | undefined;
};

export const ProviderLogo = ({ provider, contextPresent }: Props) => {
  const { t } = useTranslate();
  const getLogo = useServiceImg();

  const providerLogo = getLogo(provider, contextPresent);

  if (provider === 'TOLGEE' && contextPresent) {
    return (
      <Tooltip title={t('translation-context-present-hint')} disableInteractive>
        <StyledSource>
          {providerLogo && <img src={providerLogo} width="20px" />}
        </StyledSource>
      </Tooltip>
    );
  }

  return (
    <StyledSource>
      {providerLogo && <img src={providerLogo} width="16px" />}
    </StyledSource>
  );
};
