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
import { useDebounce } from 'use-debounce';
import { AlertTriangle } from '@untitled-ui/icons-react';
import { messageService } from 'tg.service/MessageService';

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
          messageService.success(<T keyName="project_transferred_message" />);
        },
        onError(e) {
          throw e;
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
        <T keyName="transfer_project_dialog_title" />
      </DialogTitle>
      <DialogContent>
        <Box minWidth={500} mb={2}>
          <Alert severity="warning" icon={<AlertTriangle />}>
            <T keyName="tranfer_project_dialog_warning" />
          </Alert>
          <Autocomplete
            id="transfer-project-owner-select"
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
                  {option.name} (<T keyName="transfer_option_organization" />)
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
                label={<T keyName="project_transfer_autocomplete_label" />}
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
              <T keyName="project_transfer_rewrite_project_name_to_confirm_message" />
            </Typography>
            <TextField
              variant="standard"
              data-cy="project-transfer-confirmation-field"
              onChange={(e) =>
                setRewrote(e.target.value === project.name.toUpperCase())
              }
              fullWidth
              label={
                <T
                  keyName="hard_mode_confirmation_rewrite_text"
                  params={{ text: project.name.toUpperCase() }}
                />
              }
            />
          </Box>
        </Box>
      </DialogContent>
      <DialogActions>
        <Box pr={2} pb={1} display="flex">
          <Box mr={1}>
            <Button onClick={() => props.onClose()}>
              <T keyName="confirmation_dialog_cancel" />
            </Button>
          </Box>
          <Button
            color="primary"
            variant="contained"
            disabled={!rewrote || !newOwner}
            onClick={onTransfer}
            data-cy="transfer-project-apply-button"
          >
            <T keyName="transfer_project_apply_button" />
          </Button>
        </Box>
      </DialogActions>
    </Dialog>
  );
};
