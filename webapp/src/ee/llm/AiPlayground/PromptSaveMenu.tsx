import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  Menu,
  MenuItem,
  TextField,
} from '@mui/material';
import { useRef, useState } from 'react';
import { CompactListSubheader } from 'tg.component/ListComponents';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';

type PromptModel = components['schemas']['PromptModel'];

type Props = {
  projectId: number;
  data: Omit<PromptModel, 'projectId' | 'id' | 'name'>;
};

export const PromptSaveMenu = ({ projectId, data }: Props) => {
  const [open, setOpen] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [name, setName] = useState('');

  const buttonRef = useRef<HTMLButtonElement>(null);
  const existingPrompts = useApiQuery({
    url: '/v2/projects/{projectId}/prompts',
    method: 'get',
    path: {
      projectId,
    },
    query: {
      size: 1000,
    },
  });

  const createPrompt = useApiMutation({
    url: '/v2/projects/{projectId}/prompts',
    method: 'post',
    invalidatePrefix: '/v2/projects/{projectId}/prompts',
  });

  const updatePrompt = useApiMutation({
    url: '/v2/projects/{projectId}/prompts/{promptId}',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/prompts',
  });

  return (
    <Box>
      <Button
        variant="outlined"
        size="small"
        color="primary"
        ref={buttonRef}
        onClick={() => setOpen(true)}
      >
        Save...
      </Button>
      {open && (
        <Menu
          open={true}
          onClose={() => setOpen(false)}
          anchorEl={buttonRef.current}
          MenuListProps={{ sx: { minWidth: 250 } }}
        >
          <MenuItem
            onClick={() => {
              setOpen(false);
              setCreateOpen(true);
            }}
          >
            Create new
          </MenuItem>
          {existingPrompts.data?.page?.totalElements !== 0 && (
            <Box display="grid">
              <CompactListSubheader>Update existing</CompactListSubheader>
              {existingPrompts.data?._embedded?.prompt?.map((item) => (
                <MenuItem
                  key={item.id}
                  onClick={() => {
                    updatePrompt.mutate(
                      {
                        path: { projectId, promptId: item.id },
                        content: {
                          'application/json': { ...data, name: item.name },
                        },
                      },
                      {
                        onSuccess() {
                          setOpen(false);
                        },
                      }
                    );
                  }}
                >
                  {item.name}
                </MenuItem>
              ))}
            </Box>
          )}
        </Menu>
      )}
      {createOpen && (
        <Dialog
          open={true}
          onClose={() => {
            setName('');
            setCreateOpen(false);
          }}
        >
          <DialogContent>
            <TextField value={name} onChange={(e) => setName(e.target.value)} />
          </DialogContent>
          <DialogActions>
            <Button
              onClick={() => {
                createPrompt.mutate(
                  {
                    path: { projectId },
                    content: { 'application/json': { ...data, name } },
                  },
                  {
                    onSuccess() {
                      setCreateOpen(false);
                    },
                  }
                );
              }}
            >
              Save
            </Button>
          </DialogActions>
        </Dialog>
      )}
    </Box>
  );
};
