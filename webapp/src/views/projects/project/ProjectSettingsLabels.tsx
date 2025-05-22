import { Box, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { useState } from 'react';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { LabelItem } from 'tg.views/projects/project/components/LabelItem';
import { LabelModal } from 'tg.views/projects/project/components/LabelModal';
import { LabelFormValues } from 'tg.views/projects/project/components/LabelForm';
import { components } from 'tg.service/apiSchema.generated';

type LabelModel = components['schemas']['LabelModel'];

export const ProjectSettingsLabels = () => {
  const { t } = useTranslate();

  const project = useProject();
  const [page, setPage] = useState(0);
  const [modalOpened, setModalOpen] = useState(false);
  const [selectedLabel, setSelectedLabel] = useState<LabelModel>();
  const editLabel = (label: LabelModel) => {
    setSelectedLabel(label);
    setModalOpen(true);
  };
  const submit = async (values: LabelFormValues) => {
    const label = selectedLabel;
    if (label) {
      await updateLabel.mutateAsync({
        path: { projectId: project.id, labelId: label.id },
        content: {
          'application/json': {
            ...values,
          },
        },
      });
    } else {
      await createLabel.mutateAsync({
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

  const createLabel = useApiMutation({
    url: '/v2/projects/{projectId}/labels',
    method: 'post',
    invalidatePrefix: '/v2/projects/{projectId}/labels',
  });

  const updateLabel = useApiMutation({
    url: '/v2/projects/{projectId}/labels/{labelId}',
    method: 'put',
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
          ></Box>
        </Box>
        <PaginatedHateoasList
          loadable={labels}
          onPageChange={setPage}
          emptyPlaceholder={
            <Box m={4} display="flex" justifyContent="center">
              <Typography color="textSecondary">
                {t('global_nothing_found')}
              </Typography>
            </Box>
          }
          renderItem={(l: LabelModel) => (
            <LabelItem label={l} onLabelEdit={() => editLabel(l)} />
          )}
        />
      </Box>
      {modalOpened && (
        <LabelModal
          open={modalOpened}
          label={selectedLabel}
          close={() => setModalOpen(false)}
          submit={submit}
        />
      )}
    </>
  );
};
