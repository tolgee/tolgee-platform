import { MenuItem, Select } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';

type Frequency =
  components['schemas']['NotificationSettingModel']['digestFrequency'];

type Props = { value: Frequency; afterChange: () => void };

export const DigestFrequencySelect = ({ value, afterChange }: Props) => {
  const { t } = useTranslate();
  const mutation = useApiMutation({
    url: '/v2/notification-settings/digest-frequency',
    method: 'put',
  });

  return (
    <Select
      size="small"
      value={value}
      data-cy="notifications-settings-digest-frequency"
      onChange={(e) =>
        mutation.mutate(
          {
            content: {
              'application/json': { frequency: e.target.value as Frequency },
            },
          },
          { onSuccess: afterChange }
        )
      }
    >
      <MenuItem value="DAILY">
        {t('settings_notifications_digest_daily', 'Once a day')}
      </MenuItem>
      <MenuItem value="OFF">
        {t('settings_notifications_digest_off', 'Never')}
      </MenuItem>
    </Select>
  );
};
