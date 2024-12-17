import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  styled,
  Tooltip,
  Typography,
} from '@mui/material';
import React, { FC, useState } from 'react';
import { components } from 'tg.service/billingApiSchema.generated';
import { PlanPublicChip } from '../../component/Plan/PlanPublicChip';
import { useDateFormatter } from 'tg.hooks/useLocale';
import { T } from '@tolgee/react';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { DateTimePicker } from '@mui/x-date-pickers';
import { Form, Formik, useField } from 'formik';
import { PropsOf } from '@emotion/react';
import {
  SearchSelect,
  SelectItem,
} from 'tg.component/searchSelect/SearchSelect';
import { useBillingApiQuery } from 'tg.service/http/useQueryApi';
import * as Yup from 'yup';

type Props = {
  item: components['schemas']['OrganizationWithSubscriptionsModel'];
};

const StyledContainer = styled(Box)`
  display: inline-block;
  margin-left: 4px;
`;

export const AdministrationSubscriptionsCloudPlan: FC<Props> = ({ item }) => {
  const [dialogOpen, setDialogOpen] = useState(false);
  return (
    <>
      <Tooltip
        title={
          <Popover
            item={item}
            onOpenAssignTrialDialog={() => setDialogOpen(true)}
          />
        }
      >
        <StyledContainer>{item.cloudSubscription?.plan.name}</StyledContainer>
      </Tooltip>
      <AssignTrialDialog
        open={dialogOpen}
        handleClose={() => setDialogOpen(false)}
      ></AssignTrialDialog>
    </>
  );
};

const Popover: FC<
  Props & {
    onOpenAssignTrialDialog: () => void;
  }
> = ({ item, onOpenAssignTrialDialog }) => {
  const formatDate = useDateFormatter();

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center' }}>
        <Typography variant={'h3'} sx={{ display: 'inline' }}>
          {item.cloudSubscription?.plan.name}
        </Typography>
        <Box ml={1}></Box>
        <PlanPublicChip isPublic={item.cloudSubscription?.plan.public} />
      </Box>
      <Box>
        <Typography variant={'body2'}>
          {item.cloudSubscription?.currentBillingPeriod}
        </Typography>
        <Typography variant={'body2'}>
          {item.cloudSubscription?.currentPeriodEnd &&
            formatDate(item.cloudSubscription?.currentPeriodEnd)}
        </Typography>
        <Button color="primary" onClick={() => onOpenAssignTrialDialog()}>
          <T keyName="administration-subscriptions-assign-trial" />
        </Button>
      </Box>
    </Box>
  );
};

const AssignTrialDialog: FC<{
  open: boolean;
  handleClose: () => void;
}> = ({ handleClose, open }) => {
  function handleSave(value) {
    console.log(value);
  }

  const currentDaPlus2weeks = new Date(Date.now() + 1000 * 60 * 60 * 24 * 14);

  return (
    <Formik
      initialValues={{
        trialEnd: currentDaPlus2weeks,
      }}
      onSubmit={handleSave}
      validationSchema={Yup.object().shape({
        trialEnd: Yup.date().required().min(new Date()),
        planId: Yup.number().required(),
      })}
    >
      {(formikProps) => (
        <Form>
          <Dialog open={open} fullWidth maxWidth="sm" onClose={handleClose}>
            <DialogTitle>
              <T keyName="administration-subscription-assign-trial-dialog-title" />
            </DialogTitle>
            <DialogContent sx={{ display: 'grid', gap: '16px' }}>
              <DateTimePickerField name="trialEnd" disablePast />
              <PlanSelector />
            </DialogContent>
            <DialogActions>
              <Button onClick={handleClose}>
                {' '}
                <T keyName="global_cancel_button" />
              </Button>
              <LoadingButton
                type="submit"
                onClick={formikProps.submitForm}
                // loading={saveDescription.isLoading}
                color="primary"
                variant="contained"
                // disabled={isTooLong}
                data-cy="project-ai-prompt-dialog-save"
              >
                <T keyName="global_form_save" />
              </LoadingButton>
            </DialogActions>
          </Dialog>
        </Form>
      )}
    </Formik>
  );
};

type DateTimePickerFieldProps = PropsOf<typeof DateTimePicker>;

export const DateTimePickerField: FC<
  { name: string } & DateTimePickerFieldProps
> = ({ name, onChange, ...otherProps }) => {
  const [field, _, helpers] = useField(name);
  return (
    <DateTimePicker
      onChange={(value) => helpers.setValue(value)}
      value={field.value}
    />
  );
};

export const PlanSelector = () => {
  const plansLoadable = useBillingApiQuery({
    url: '/v2/administration/billing/cloud-plans',
    method: 'get',
  });

  const [field, meta, helpers] = useField('planId');

  if (plansLoadable.isLoading) {
    return null;
  }

  function onChange(val) {
    helpers.setValue(val);
  }

  const plans = plansLoadable?.data?._embedded?.plans ?? [];
  const selectItems = plans
    .filter((p) => !p.free)
    .map(
      (plan) =>
        ({
          value: plan.id,
          name: plan.name,
        } satisfies SelectItem<number>)
    );

  return (
    <SearchSelect
      SelectProps={{ error: meta.error }}
      items={selectItems}
      value={field.value}
      onChange={onChange}
    />
  );
};
