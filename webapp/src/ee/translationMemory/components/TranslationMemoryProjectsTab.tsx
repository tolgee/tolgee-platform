import React, { useMemo, useState } from 'react';
import {
  Box,
  Button,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  IconButton,
  Radio,
  RadioGroup,
  styled,
  Tooltip,
  Typography,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { XClose } from '@untitled-ui/icons-react';
import { useQueryClient } from 'react-query';

type TmAssignedProjectModel = components['schemas']['TmAssignedProjectModel'];

const StyledTable = styled('div')`
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  border-radius: 8px;
  overflow: hidden;
`;

const StyledHeader = styled('div')`
  display: grid;
  grid-template-columns: 1fr 80px 80px 50px;
  background: ${({ theme }) => theme.palette.background.paper};
  padding: ${({ theme }) => theme.spacing(1, 2)};
  font-size: 11px;
  text-transform: uppercase;
  color: ${({ theme }) => theme.palette.text.secondary};
  letter-spacing: 0.04em;
  font-weight: 500;
  border-bottom: 1px solid ${({ theme }) => theme.palette.divider1};
`;

const StyledRow = styled('div')`
  display: grid;
  grid-template-columns: 1fr 80px 80px 50px;
  align-items: center;
  padding: ${({ theme }) => theme.spacing(1.5, 2)};
  border-top: 1px solid ${({ theme }) => theme.palette.divider1};
  &:first-of-type {
    border-top: none;
  }
`;

const StyledProjectName = styled('div')`
  font-weight: 500;
`;

type Props = {
  organizationId: number;
  translationMemoryId: number;
};

export const TranslationMemoryProjectsTab: React.VFC<Props> = ({
  organizationId,
  translationMemoryId,
}) => {
  const { t } = useTranslate();
  const queryClient = useQueryClient();

  const assignedProjects = useApiQuery({
    url: '/v2/organizations/{organizationId}/translation-memories/{translationMemoryId}/assigned-projects',
    method: 'get',
    path: { organizationId, translationMemoryId },
  });

  const items = useMemo(
    () => assignedProjects.data?._embedded?.assignedProjects ?? [],
    [assignedProjects.data]
  );

  const invalidate = () => {
    assignedProjects.refetch();
    queryClient.invalidateQueries(
      '/v2/organizations/{organizationId}/translation-memories'
    );
  };

  const updateMutation = useApiMutation({
    url: '/v2/projects/{projectId}/translation-memories/{translationMemoryId}',
    method: 'put',
  });

  const unassignMutation = useApiMutation({
    url: '/v2/projects/{projectId}/translation-memories/{translationMemoryId}',
    method: 'delete',
  });

  const toggleAccess = (
    item: TmAssignedProjectModel,
    field: 'readAccess' | 'writeAccess'
  ) => {
    updateMutation.mutate(
      {
        path: { projectId: item.projectId, translationMemoryId },
        content: {
          'application/json': {
            readAccess:
              field === 'readAccess' ? !item.readAccess : item.readAccess,
            writeAccess:
              field === 'writeAccess' ? !item.writeAccess : item.writeAccess,
            priority: item.priority,
          },
        },
      },
      { onSuccess: invalidate }
    );
  };

  const [disconnectItem, setDisconnectItem] =
    useState<TmAssignedProjectModel | null>(null);
  const [keepData, setKeepData] = useState(false);

  const handleRemove = (item: TmAssignedProjectModel) => {
    setKeepData(false);
    setDisconnectItem(item);
  };

  const handleDisconnectConfirm = () => {
    if (!disconnectItem) return;
    unassignMutation.mutate(
      {
        path: { projectId: disconnectItem.projectId, translationMemoryId },
        query: { keepData },
      },
      {
        onSuccess: () => {
          setDisconnectItem(null);
          invalidate();
        },
      }
    );
  };

  return (
    <Box>
      <Box
        display="flex"
        justifyContent="space-between"
        alignItems="center"
        mb={2}
      >
        <Typography variant="body2" color="textSecondary">
          <T
            keyName="translation_memory_projects_description"
            defaultValue="Projects using this translation memory. Configure read/write access per project."
          />
        </Typography>
      </Box>

      {items.length === 0 ? (
        <Box py={4} textAlign="center" color="text.secondary">
          <T
            keyName="translation_memory_no_assigned_projects"
            defaultValue="No projects are using this translation memory yet."
          />
        </Box>
      ) : (
        <StyledTable data-cy="tm-assigned-projects-table">
          <StyledHeader>
            <div>{t('translation_memory_projects_col_project', 'Project')}</div>
            <div style={{ textAlign: 'center' }}>
              {t('translation_memory_projects_col_read', 'Read')}
            </div>
            <div style={{ textAlign: 'center' }}>
              {t('translation_memory_projects_col_write', 'Write')}
            </div>
            <div />
          </StyledHeader>
          {items.map((item) => (
            <StyledRow key={item.projectId} data-cy="tm-assigned-project-row">
              <StyledProjectName>{item.projectName}</StyledProjectName>
              <Box textAlign="center">
                <Checkbox
                  checked={item.readAccess}
                  onChange={() => toggleAccess(item, 'readAccess')}
                  size="small"
                  data-cy="tm-project-read-checkbox"
                />
              </Box>
              <Box textAlign="center">
                <Checkbox
                  checked={item.writeAccess}
                  onChange={() => toggleAccess(item, 'writeAccess')}
                  size="small"
                  data-cy="tm-project-write-checkbox"
                />
              </Box>
              <Box textAlign="center">
                <Tooltip
                  title={t('translation_memory_remove_project', 'Remove')}
                >
                  <IconButton
                    size="small"
                    onClick={() => handleRemove(item)}
                    data-cy="tm-project-remove"
                  >
                    <XClose width={16} height={16} />
                  </IconButton>
                </Tooltip>
              </Box>
            </StyledRow>
          ))}
        </StyledTable>
      )}

      <Dialog
        open={!!disconnectItem}
        onClose={() => setDisconnectItem(null)}
        data-cy="tm-disconnect-dialog"
      >
        <DialogTitle>
          <T
            keyName="translation_memory_disconnect_title"
            defaultValue="Remove project from translation memory"
          />
        </DialogTitle>
        <DialogContent>
          <Typography variant="body2" mb={2}>
            <T
              keyName="translation_memory_disconnect_message"
              defaultValue='You are about to disconnect "{projectName}" from this translation memory. What should happen to the existing entries?'
              params={{ projectName: disconnectItem?.projectName ?? '' }}
            />
          </Typography>
          <RadioGroup
            value={keepData ? 'keep' : 'discard'}
            onChange={(e) => setKeepData(e.target.value === 'keep')}
          >
            <FormControlLabel
              value="discard"
              control={<Radio data-cy="tm-disconnect-discard" />}
              label={t(
                'translation_memory_disconnect_discard',
                'Just disconnect — entries stay in the shared memory'
              )}
            />
            <FormControlLabel
              value="keep"
              control={<Radio data-cy="tm-disconnect-keep" />}
              label={t(
                'translation_memory_disconnect_keep',
                "Copy entries into the project's own memory before disconnecting"
              )}
            />
          </RadioGroup>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDisconnectItem(null)}>
            <T keyName="global_cancel_button" defaultValue="Cancel" />
          </Button>
          <Button
            variant="contained"
            color="primary"
            onClick={handleDisconnectConfirm}
            disabled={unassignMutation.isLoading}
            data-cy="tm-disconnect-confirm"
          >
            <T
              keyName="translation_memory_disconnect_confirm"
              defaultValue="Remove"
            />
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};
