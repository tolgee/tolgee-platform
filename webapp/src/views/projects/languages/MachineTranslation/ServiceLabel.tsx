import { Box, Tooltip } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { useServiceImg } from 'tg.views/projects/translations/ToolsPanel/panels/MachineTranslation/useServiceImg';
import { getServiceName } from './getServiceName';
import { ServiceType } from './types';

type Props = {
  service: ServiceType;
  isSupported: boolean;
};

export const ServiceLabel = ({ service, isSupported }: Props) => {
  const getServiceImg = useServiceImg();
  const { t } = useTranslate();
  return (
    <Tooltip
      title={!isSupported ? t('project_mt_dialog_service_not_supported') : ''}
      disableInteractive
    >
      <Box
        display="flex"
        gap={1}
        sx={{ opacity: isSupported ? 1 : 0.5 }}
        data-cy="mt-language-dialog-service-label"
      >
        <img src={getServiceImg(service, false)} width={20} />
        <div>{getServiceName(service)}</div>
      </Box>
    </Tooltip>
  );
};
