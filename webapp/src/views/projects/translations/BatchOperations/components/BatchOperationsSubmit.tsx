import { ChevronRight } from '@mui/icons-material';
import LoadingButton from 'tg.component/common/form/LoadingButton';

export const BatchOperationsSubmit = (
  props: React.ComponentProps<typeof LoadingButton>
) => {
  return (
    <LoadingButton
      data-cy="batch-operations-submit-button"
      disableElevation
      variant="contained"
      color="primary"
      sx={{ minWidth: 0, minHeight: 0, width: 40, height: 40 }}
      {...props}
    >
      <ChevronRight />
    </LoadingButton>
  );
};
