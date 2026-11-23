import { FormControl, Select, styled, Typography } from '@mui/material';
import { ActivityTreeSelect as Props } from './activityStreamQuery';
import { activityTreeOptions } from './ActivityFilterOptions';

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

export const ActivityTreeSelect = ({
  summary,
  ...props
}: Props & { 'data-cy': string }) => (
  <FormControl variant="outlined" size="small" data-cy={props['data-cy']}>
    <StyledSelect
      multiple
      displayEmpty
      value={props.value}
      renderValue={() => (
        <StyledTypography
          color={props.value.length ? 'textPrimary' : 'textSecondary'}
          variant="body1"
        >
          {summary}
        </StyledTypography>
      )}
      MenuProps={{
        variant: 'menu',
        PaperProps: { sx: { minWidth: 280, maxWidth: 400, maxHeight: 480 } },
      }}
    >
      {activityTreeOptions(props)}
    </StyledSelect>
  </FormControl>
);
