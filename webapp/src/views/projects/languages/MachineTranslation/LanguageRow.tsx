import React, { useState } from 'react';
import { Box, IconButton } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Settings01 } from '@untitled-ui/icons-react';

import { LanguageItem } from '../../../../component/languages/LanguageItem';
import {
  TABLE_CENTERED,
  TABLE_FIRST_CELL,
  TABLE_LAST_CELL,
} from '../../../../component/languages/tableStyles';
import { ServiceAvatar } from './ServiceAvatar';
import { LanguageSettingsDialog } from './LanguageSettingsDialog';
import { RowData, ServiceType } from './types';

type Props = {
  rowData: RowData;
};

export const LanguageRow: React.FC<Props> = ({ rowData }) => {
  const { t } = useTranslate();

  const { inheritedFromDefault, settings } = rowData;

  const [settingsOpen, setSettingsOpen] = useState(false);

  function isSupported(service: ServiceType | undefined) {
    return (
      service &&
      (!settings.info ||
        settings.info.supportedServices.find((i) => i.serviceType === service))
    );
  }

  return (
    <>
      <div className={TABLE_FIRST_CELL} style={{ minWidth: 200 }}>
        {settings.language ? (
          <LanguageItem language={settings.language} />
        ) : (
          t('project_languages_default_settings')
        )}
      </div>
      <Box className={TABLE_CENTERED}>
        <ServiceAvatar
          language={rowData.settings.language?.tag || 'default'}
          service={settings.mtSettings?.primaryServiceInfo?.serviceType}
          inheritedFromDefault={inheritedFromDefault}
          notSupported={
            !isSupported(settings.mtSettings?.primaryServiceInfo?.serviceType)
          }
          data-cy="machine-translations-settings-language-primary-service"
          data-cy-language={rowData.settings.language?.tag || 'default'}
          data-cy-service={settings.mtSettings?.primaryServiceInfo?.serviceType}
        />
      </Box>
      <Box display="flex" gap={3} sx={{ padding: '0px 15px' }}>
        {settings.mtSettings?.enabledServicesInfo
          .filter(
            ({ serviceType }) =>
              !rowData.settings.info ||
              rowData.settings.info?.supportedServices.find(
                (s) => s.serviceType === serviceType
              )
          )
          .map(({ serviceType }) => (
            <div key={serviceType} className={TABLE_CENTERED}>
              <ServiceAvatar
                language={rowData.settings.language?.tag || 'default'}
                service={serviceType}
                inheritedFromDefault={inheritedFromDefault}
                data-cy="machine-translations-settings-language-enabled-service"
                data-cy-language={rowData.settings.language?.tag || 'default'}
                data-cy-service={serviceType}
              />
            </div>
          ))}
      </Box>
      <Box className={TABLE_LAST_CELL}>
        <IconButton
          onClick={() => setSettingsOpen(true)}
          size="small"
          data-cy="machine-translations-settings-language-options"
          data-cy-language={rowData.settings.language?.tag || 'default'}
        >
          <Settings01 />
        </IconButton>
      </Box>
      {settingsOpen && (
        <LanguageSettingsDialog
          onClose={() => setSettingsOpen(false)}
          rowData={rowData}
        />
      )}
    </>
  );
};
