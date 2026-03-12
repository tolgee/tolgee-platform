import {
  Box,
  FormControl,
  MenuItem,
  Select,
  SelectChangeEvent,
  styled,
  Typography,
} from '@mui/material';
import { QaSettingsItemTooltip } from 'tg.ee.module/qa/components/QaSettingsItemTooltip';
import { components } from 'tg.service/apiSchema.generated';
import { useTranslate } from '@tolgee/react';
import { useQaCheckTypeLabel } from 'tg.ee.module/qa/hooks/useQaCheckTypeLabel';

type QaCheckType = components['schemas']['QaIssueModel']['type'];
type QaSettings = components['schemas']['QaSettingsRequest'];
type QaCheckSeverity = QaSettings['settings'][keyof QaSettings['settings']];

const SENTINEL_DEFAULT = '__DEFAULT__';

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

type BaseProps = {
  type: QaCheckType;
};

type WithoutDefaultProps = BaseProps & {
  showDefault?: false;
  value: QaCheckSeverity;
  onChange?: (type: QaCheckType, severity: QaCheckSeverity) => void;
};

type WithDefaultProps = BaseProps & {
  showDefault: true;
  value: QaCheckSeverity | null;
  onChange?: (type: QaCheckType, severity: QaCheckSeverity | null) => void;
};

type Props = WithoutDefaultProps | WithDefaultProps;

export const QaSettingsItem = ({
  type,
  value,
  onChange,
  showDefault = false,
}: Props) => {
  const { t } = useTranslate();
  const label = useQaCheckTypeLabel(type);

  const selectValue = value === null ? SENTINEL_DEFAULT : value;

  const handleOnChange = (
    e: SelectChangeEvent<QaCheckSeverity | typeof SENTINEL_DEFAULT>
  ) => {
    if (!onChange) return;
    const raw = e.target.value;
    if (showDefault) {
      const value = raw === SENTINEL_DEFAULT ? null : (raw as QaCheckSeverity);
      (onChange as WithDefaultProps['onChange'])!(type, value);
    } else {
      onChange(type, raw as QaCheckSeverity);
    }
  };

  return (
    <StyledRow data-cy="qa-settings-row" data-cy-type={type}>
      <StyledLabel>
        <Typography variant="body1">{label}</Typography>
        <QaSettingsItemTooltip type={type} />
      </StyledLabel>
      <FormControl size="small" sx={{ minWidth: 120 }}>
        <Select
          value={selectValue}
          onChange={handleOnChange}
          data-cy="qa-settings-select"
          data-cy-type={type}
        >
          {showDefault && (
            <MenuItem value={SENTINEL_DEFAULT}>
              {t('project_settings_qa_severity_default')}
            </MenuItem>
          )}
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
