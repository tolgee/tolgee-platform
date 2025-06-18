import { Box, Button, styled, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { useState } from 'react';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { LabelItem } from 'tg.views/projects/project/components/Labels/LabelItem';
import { LabelModal } from 'tg.views/projects/project/components/Labels/LabelModal';
import { LabelFormValues } from 'tg.views/projects/project/components/Labels/LabelForm';
import { components } from 'tg.service/apiSchema.generated';
import { Plus } from '@untitled-ui/icons-react';
import { confirmation } from 'tg.hooks/confirmation';

type LabelModel = components['schemas']['LabelModel'];

const TableGrid = styled('div')`
  display: grid;
  grid-template-columns: auto 1fr auto;
`;

export const ProjectSettingsLabels = () => {
  const { t } = useTranslate();

  const project = useProject();
  const [page, setPage] = useState(0);
  const [modalOpened, setModalOpen] = useState(false);
  const [selectedLabel, setSelectedLabel] = useState<LabelModel>();
  const addLabel = () => {
    setSelectedLabel(undefined);
    setModalOpen(true);
  };
  const editLabel = (label: LabelModel) => {
    setSelectedLabel(label);
    setModalOpen(true);
  };
  const removeLabel = async (label: LabelModel) => {
    confirmation({
      message: <T keyName="project_settings_label_delete_confirmation" />,
      async onConfirm() {
        await removeMutation.mutateAsync({
          path: { projectId: project.id, labelId: label.id },
        });
      },
    });
  };
  const submit = async (values: LabelFormValues) => {
    const label = selectedLabel;
    if (label) {
      await updateMutation.mutateAsync({
        path: { projectId: project.id, labelId: label.id },
        content: {
          'application/json': {
            ...values,
          },
        },
      });
    } else {
      await createMutation.mutateAsync({
        path: { projectId: project.id },
        content: {
          'application/json': {
            ...values,
          },
        },
      });
    }
    setModalOpen(false);
  };

  const labels = useApiQuery({
    url: '/v2/projects/{projectId}/labels',
    method: 'get',
    path: { projectId: project.id },
    query: {
      page,
      sort: ['name'],
    },
    options: {
      keepPreviousData: true,
    },
  });

  const createMutation = useApiMutation({
    url: '/v2/projects/{projectId}/labels',
    method: 'post',
    invalidatePrefix: '/v2/projects/{projectId}/labels',
  });

  const updateMutation = useApiMutation({
    url: '/v2/projects/{projectId}/labels/{labelId}',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/labels',
  });

  const removeMutation = useApiMutation({
    url: '/v2/projects/{projectId}/labels/{labelId}',
    method: 'delete',
    invalidatePrefix: '/v2/projects/{projectId}/labels',
  });

  return (
    <>
      <Box>
        <Box
          mt={2}
          mb={1}
          display="flex"
          justifyContent="right"
          alignItems="center"
        >
          <Box
            display="flex"
            gap={2}
            justifyContent="end"
            mb={1}
            alignItems="stretch"
          >
            <Button
              variant="contained"
              color="primary"
              startIcon={<Plus width={19} height={19} />}
              onClick={addLabel}
              data-cy="project-settings-labels-add-button"
            >
              {t('add_label')}
            </Button>
          </Box>
        </Box>
        <PaginatedHateoasList
          loadable={labels}
          onPageChange={setPage}
          listComponent={TableGrid}
          data-cy="project-settings-labels-list"
          emptyPlaceholder={
            <Box m={2} display="flex" justifyContent="center">
              <Typography color="textSecondary">
                {t('project_settings_no_labels_yet')}
              </Typography>
            </Box>
          }
          renderItem={(l: LabelModel) => (
            <LabelItem
              label={l}
              onLabelEdit={() => editLabel(l)}
              onLabelRemove={() => removeLabel(l)}
            />
          )}
        />
      </Box>
      <LabelModal
        open={modalOpened}
        label={selectedLabel}
        close={() => setModalOpen(false)}
        submit={submit}
      />
    </>
  );
};
