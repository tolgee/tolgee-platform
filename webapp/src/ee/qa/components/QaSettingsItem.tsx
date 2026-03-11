import {
  Box,
  FormControl,
  MenuItem,
  Select,
  styled,
  Typography,
} from '@mui/material';
import { QaSettingsItemTooltip } from 'tg.ee.module/qa/components/QaSettingsItemTooltip';
import { components } from 'tg.service/apiSchema.generated';
import { useTranslate } from '@tolgee/react';
import { useQaCheckTypeLabel } from 'tg.ee.module/qa/hooks/useQaCheckTypeLabel';

type QaCheckResultItem = components['schemas']['QaIssueModel']['type']
type QaSettings = components['schemas']['QaSettingsRequest'];
type QaCheckSeverity = QaSettings['settings'][keyof QaSettings['settings']];

const StyledRow = styled(Box)`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
`;

const StyledLabel = styled(Box)`
  display: flex;
  align-items: center;
  gap: 4px;
`;

type Props = {
  type: QaCheckResultItem;
  value: QaCheckSeverity;
  onChange?: (type: string, severity: QaCheckSeverity) => void;
};

export const QaSettingsItem = ({ type, value, onChange }: Props) => {
  const { t } = useTranslate();
  const label = useQaCheckTypeLabel(type);
  return (
    <StyledRow data-cy="qa-settings-row" data-cy-type={type}>
      <StyledLabel>
        <Typography variant="body1">{label}</Typography>
        <QaSettingsItemTooltip type={type} />
      </StyledLabel>
      <FormControl size="small" sx={{ minWidth: 120 }}>
        <Select
          value={value}
          onChange={(e) =>
            onChange && onChange(type, e.target.value as QaCheckSeverity)
          }
          data-cy="qa-settings-select"
          data-cy-type={type}
        >
          <MenuItem value="WARNING">
            {t('project_settings_qa_severity_warning')}
          </MenuItem>
          <MenuItem value="OFF">
            {t('project_settings_qa_severity_off')}
          </MenuItem>
        </Select>
      </FormControl>
    </StyledRow>
  );
};
