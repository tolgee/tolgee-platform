import { ChevronRight } from '@untitled-ui/icons-react';
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
      sx={{ minWidth: 0, minHeight: 0, width: 40, height: 40, padding: 0 }}
      {...props}
    >
      <ChevronRight width={20} height={20} />
    </LoadingButton>
  );
};
