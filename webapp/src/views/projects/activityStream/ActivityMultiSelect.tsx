import { FormControl, Select, styled, Typography } from '@mui/material';
import { ActivityFilterSelect, sameSet } from './activityStreamQuery';
import { activityFilterOptions } from './ActivityFilterOptions';

const StyledSelect = styled(Select)`
  width: 200px;
  height: 40px;
  & .MuiSelect-select {
    display: flex;
    align-items: center;
  }
`;

const StyledTypography = styled(Typography)`
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  margin: -2px 0px;
`;

type Props = ActivityFilterSelect & { 'data-cy': string };

export const ActivityMultiSelect = ({
  value,
  onChange,
  options,
  presets,
  noneLabel,
  ...rest
}: Props) => {
  const labels = new Map(options.map((o) => [o.value, o.label]));

  const renderLabel = () => {
    const preset = presets.find((p) => sameSet(p.values, value));
    if (preset) return preset.label;
    if (!value.length) return noneLabel;
    return value.map((v) => labels.get(v) ?? v).join(', ');
  };

  return (
    <FormControl variant="outlined" size="small" data-cy={rest['data-cy']}>
      <StyledSelect
        multiple
        displayEmpty
        value={value}
        renderValue={() => (
          <StyledTypography
            color={value.length ? 'textPrimary' : 'textSecondary'}
            variant="body1"
          >
            {renderLabel()}
          </StyledTypography>
        )}
        MenuProps={{
          variant: 'menu',
          PaperProps: { sx: { minWidth: 200, maxHeight: 420 } },
        }}
      >
        {activityFilterOptions({ value, onChange, options, presets })}
      </StyledSelect>
    </FormControl>
  );
};
