import { Warning } from '@mui/icons-material';
import { Box, styled, Tooltip } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { useServiceImg } from 'tg.views/projects/translations/TranslationTools/useServiceImg';
import { getServiceName } from './getServiceName';
import { ServiceType } from './types';

const StyledWarning = styled(Warning)`
  font-size: 16px;
  position: absolute;
  top: -8px;
  right: -8px;
  color: ${({ theme }) => theme.palette.error.main};
`;

type Props = {
  inheritedFromDefault: boolean;
  service: ServiceType | undefined;
  notSupported?: boolean;
  language: string;
  'data-cy': string;
};

export const ServiceAvatar = ({
  service,
  inheritedFromDefault,
  notSupported,
  language,
  'data-cy': dataCy,
}: Props) => {
  const getServiceImg = useServiceImg();
  const { t } = useTranslate();

  return (
    <>
      {service && (
        <Tooltip
          title={
            notSupported
              ? t('project_languages_mt_settings_provider_not_supported')
              : inheritedFromDefault
              ? t('project_languages_mt_settings_inherited')
              : getServiceName(service)
          }
          disableInteractive
        >
          <Box
            display="flex"
            alignItems="center"
            position="relative"
            data-cy={dataCy}
            data-cy-service={service}
            data-cy-language={language}
          >
            <img
              style={{ opacity: inheritedFromDefault ? '0.5' : '1' }}
              src={getServiceImg(service, false) || undefined}
              width="22px"
            />
            {notSupported && <StyledWarning />}
          </Box>
        </Tooltip>
      )}
    </>
  );
};
