import { Box, Tooltip } from '@mui/material';
import { useTranslate } from '@tolgee/react';

type Props = {
  isSupported: boolean;
  icon: string | undefined;
  name: string;
};

export const ServiceLabel = ({ isSupported, icon, name }: Props) => {
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
        <img src={icon} width={20} />
        <div>{name}</div>
      </Box>
    </Tooltip>
  );
};
