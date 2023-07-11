import { ChevronRight } from '@mui/icons-material';
import { useTranslate } from '@tolgee/react';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { confirmation } from 'tg.hooks/confirmation';
import { useProject } from 'tg.hooks/useProject';
import { useApiMutation } from 'tg.service/http/useQueryApi';

import { useTranslationsSelector } from '../context/TranslationsContext';
import { OperationProps } from './types';

type Props = OperationProps;
export const OperationDelete = ({ disabled, onStart }: Props) => {
  const { t } = useTranslate();
  const selection = useTranslationsSelector((c) => c.selection);
  const project = useProject();
  const batchLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/start-batch-job/delete-keys',
    method: 'post',
  });

  function handleSubmit() {
    confirmation({
      title: t('translations_delete_selected'),
      message: t('translations_key_delete_confirmation_text', {
        count: String(selection.length),
      }),
      onConfirm() {
        batchLoadable.mutate(
          {
            path: { projectId: project.id },
            content: {
              'application/json': {
                keyIds: selection,
              },
            },
          },
          {
            onSuccess(data) {
              onStart(data);
            },
          }
        );
      },
    });
  }

  return (
    <LoadingButton
      data-cy="batch-operations-submit-button"
      onClick={handleSubmit}
      loading={batchLoadable.isLoading}
      disabled={disabled}
      sx={{ minWidth: 0, minHeight: 0, width: 40, height: 40 }}
      variant="contained"
      color="primary"
    >
      <ChevronRight />
    </LoadingButton>
  );
};
