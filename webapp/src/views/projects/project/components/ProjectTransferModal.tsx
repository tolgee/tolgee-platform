import {
  Alert,
  Autocomplete,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  MenuItem,
  TextField,
  Typography,
} from '@mui/material';
import React, { FC, useState } from 'react';
import { T } from '@tolgee/react';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { useProject } from 'tg.hooks/useProject';
import { useDebounce } from 'use-debounce/lib';
import { Warning } from '@mui/icons-material';
import { container } from 'tsyringe';
import { MessageService } from 'tg.service/MessageService';

const messaging = container.resolve(MessageService);

export const ProjectTransferModal: FC<{
  open: boolean;
  onClose: () => void;
}> = (props) => {
  const project = useProject();

  const [search, setSearch] = useState(undefined as string | undefined);

  const [debouncedSearch] = useDebounce(search, 500);
  const [rewrote, setRewrote] = useState(false);
  const [newOwner, setNewOwner] = useState(
    undefined as undefined | components['schemas']['ProjectTransferOptionModel']
  );

  const transferToOrganization = useApiMutation({
    url: '/v2/projects/{projectId}/transfer-to-organization/{organizationId}',
    method: 'put',
    invalidatePrefix: '/v2/projects',
  });

  const onTransfer = () => {
    transferToOrganization.mutate(
      {
        path: {
          projectId: project.id,
          organizationId: newOwner!.id,
        },
      },
      {
        onSuccess() {
          messaging.success(<T>project_transferred_message</T>);
        },
        onError(e) {
          throw new Error(e);
        },
      }
    );
    props.onClose();
  };

  const optionsLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/transfer-options',
    method: 'get',
    path: {
      projectId: project.id,
    },
    query: {
      search: debouncedSearch,
    },
  });

  const options = optionsLoadable?.data?._embedded?.transferOptions || [];

  const onSelect = (
    option: components['schemas']['ProjectTransferOptionModel']
  ) => {
    setNewOwner(option);
  };

  return (
    <Dialog
      open={props.open}
      onClose={props.onClose}
      data-cy={'project-transfer-dialog'}
    >
      <DialogTitle id="transfer-dialog-title">
        <T>transfer_project_dialog_title</T>
      </DialogTitle>
      <DialogContent>
        <Box minWidth={500} mb={2}>
          <Alert severity="warning" icon={<Warning />}>
            <T>tranfer_project_dialog_warning</T>
          </Alert>
          <Autocomplete
            id="transfer-project-owner-select"
            freeSolo
            options={options}
            getOptionLabel={(option) => option.name || ''}
            filterOptions={(options) => {
              return options;
            }}
            onChange={(_, value) => {
              onSelect(
                value as components['schemas']['ProjectTransferOptionModel']
              );
            }}
            renderOption={(props, option) => (
              <MenuItem {...props}>
                <span data-cy="project-transfer-autocomplete-suggested-option">
                  {option.name} (<T>transfer_option_organization</T>)
                </span>
              </MenuItem>
            )}
            renderInput={(params) => (
              <TextField
                variant="standard"
                data-cy="project-transfer-autocomplete-field"
                {...params}
                onChange={(e) => {
                  setSearch(e.target.value);
                }}
                label={<T>project_transfer_autocomplete_label</T>}
                margin="normal"
                InputProps={{
                  ...params.InputProps,
                }}
              />
            )}
          />
        </Box>
      </DialogContent>
      <Divider />
      <DialogContent>
        <Box>
          <Box mt={2} mb={1}>
            <Typography>
              <T>project_transfer_rewrite_project_name_to_confirm_message</T>
            </Typography>
            <TextField
              variant="standard"
              data-cy="project-transfer-confirmation-field"
              onChange={(e) =>
                setRewrote(e.target.value === project.name.toUpperCase())
              }
              fullWidth
              label={
                <T params={{ text: project.name.toUpperCase() }}>
                  hard_mode_confirmation_rewrite_text
                </T>
              }
            />
          </Box>
        </Box>
      </DialogContent>
      <DialogActions>
        <Box pr={2} pb={1} display="flex">
          <Box mr={1}>
            <Button onClick={() => props.onClose()}>
              <T>confirmation_dialog_cancel</T>
            </Button>
          </Box>
          <Button
            color="primary"
            variant="contained"
            disabled={!rewrote || !newOwner}
            onClick={onTransfer}
            data-cy="transfer-project-apply-button"
          >
            <T>transfer_project_apply_button</T>
          </Button>
        </Box>
      </DialogActions>
    </Dialog>
  );
};
