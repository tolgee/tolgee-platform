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
  padding: ${({ theme }) => theme.spacing(1, 0)};
`;

const StyledLabel = styled(Box)`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(0.5)};
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
  globalDefault: QaCheckSeverity;
  onChange?: (type: QaCheckType, severity: QaCheckSeverity | null) => void;
};

type Props = WithoutDefaultProps | WithDefaultProps;

export const QaSettingsItem = (props: Props) => {
  const { type, value, onChange, showDefault = false } = props;
  const { t } = useTranslate();
  const label = useQaCheckTypeLabel(type);
  const globalDefault =
    'globalDefault' in props ? props.globalDefault : undefined;

  const severityLabel = (severity: QaCheckSeverity) => {
    switch (severity) {
      case 'WARNING':
        return t('project_settings_qa_severity_warning');
      case 'OFF':
        return t('project_settings_qa_severity_off');
      default:
        return severity;
    }
  };

  const selectValue = value === null ? SENTINEL_DEFAULT : value;

  const handleOnChange = (
    e: SelectChangeEvent<QaCheckSeverity | typeof SENTINEL_DEFAULT>
  ) => {
    if (!onChange) return;
    const raw = e.target.value;
    if (showDefault) {
      const value = raw === SENTINEL_DEFAULT ? null : (raw as QaCheckSeverity);
      (onChange as NonNullable<WithDefaultProps['onChange']>)(type, value);
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
      <FormControl size="small" sx={{ minWidth: showDefault ? 180 : 120 }}>
        <Select
          value={selectValue}
          onChange={handleOnChange}
          data-cy="qa-settings-select"
          data-cy-type={type}
        >
          {showDefault && globalDefault && (
            <MenuItem value={SENTINEL_DEFAULT}>
              {t(
                'project_settings_qa_severity_default_with_value',
                'Default ({value})',
                { value: severityLabel(globalDefault) }
              )}
            </MenuItem>
          )}
          <MenuItem value="WARNING">{severityLabel('WARNING')}</MenuItem>
          <MenuItem value="OFF">{severityLabel('OFF')}</MenuItem>
        </Select>
      </FormControl>
    </StyledRow>
  );
};
