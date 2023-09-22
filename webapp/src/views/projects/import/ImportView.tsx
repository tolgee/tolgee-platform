import { FunctionComponent, useEffect, useState } from 'react';
import { Box } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { container } from 'tsyringe';

import { LINKS, PARAMS } from 'tg.constants/links';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { confirmation } from 'tg.hooks/confirmation';
import { useProject } from 'tg.hooks/useProject';
import { MessageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import LoadingButton from 'tg.component/common/form/LoadingButton';

import { ImportAlertError } from './ImportAlertError';
import { ImportConflictNotResolvedErrorDialog } from './component/ImportConflictNotResolvedErrorDialog';
import { ImportConflictResolutionDialog } from './component/ImportConflictResolutionDialog';
import ImportFileInput from './component/ImportFileInput';
import { ImportResult } from './component/ImportResult';
import { useApplyImportHelper } from './hooks/useApplyImportHelper';
import { useImportDataHelper } from './hooks/useImportDataHelper';
import { BaseProjectView } from '../BaseProjectView';

const messageService = container.resolve(MessageService);

export const ImportView: FunctionComponent = () => {
  const dataHelper = useImportDataHelper();

  const project = useProject();

  const applyImportHelper = useApplyImportHelper(dataHelper);

  const [resolveRow, setResolveRow] = useState(
    undefined as components['schemas']['ImportLanguageModel'] | undefined
  );

  const { refetchUsage } = useGlobalActions();

  const { t } = useTranslate();

  const onConflictResolutionDialogClose = () => {
    dataHelper.refetchData();
    setResolveRow(undefined);
  };

  const resolveFirstUnresolved = () => {
    const row = dataHelper.result?._embedded?.languages?.find(
      (l) => l.conflictCount > l.resolvedCount
    );
    setResolveRow(row);
  };

  useEffect(() => {
    if (applyImportHelper.error) {
      const parsed = parseErrorResponse(applyImportHelper.error);
      messageService.error(<TranslatedError code={parsed[0]} />);
    }
  }, [applyImportHelper.error]);

  const onApply = () => {
    dataHelper.touchApply();
    if (dataHelper.isValid) {
      applyImportHelper.onApplyImport();
    }
  };

  useEffect(() => {
    if (!applyImportHelper.loading && applyImportHelper.loaded) {
      refetchUsage();
    }
  }, [applyImportHelper.loading, applyImportHelper.loaded]);

  return (
    <BaseProjectView
      windowTitle={t('import_translations_title')}
      navigation={[
        [
          t('import_translations_title'),
          LINKS.PROJECT_IMPORT.build({
            [PARAMS.PROJECT_ID]: project.id,
          }),
        ],
      ]}
      maxWidth="wide"
    >
      <ImportConflictResolutionDialog
        row={resolveRow}
        onClose={onConflictResolutionDialogClose}
      />
      <Box mt={2} position="relative">
        <ImportFileInput
          onNewFiles={dataHelper.onNewFiles}
          loading={dataHelper.addFilesMutation.isLoading}
        />

        {dataHelper.addFilesMutation.data?.errors?.map((e, idx) => (
          <ImportAlertError
            key={idx}
            error={e}
            addFilesMutation={dataHelper.addFilesMutation}
          />
        ))}
        <ImportResult
          onResolveRow={setResolveRow}
          onLoadData={dataHelper.refetchData}
          result={dataHelper.result}
        />
      </Box>
      {dataHelper.result && (
        <Box display="flex" mt={2} justifyContent="flex-end">
          <Box mr={2}>
            <LoadingButton
              loading={dataHelper.cancelMutation.isLoading}
              data-cy="import_cancel_import_button"
              variant="outlined"
              color="primary"
              onClick={() => {
                confirmation({
                  onConfirm: () => dataHelper.onCancel(),
                  title: <T keyName="import_cancel_confirmation_title" />,
                  message: <T keyName="import_cancel_confirmation_message" />,
                });
              }}
            >
              <T keyName="import_cancel_button" />
            </LoadingButton>
          </Box>
          <Box>
            <LoadingButton
              variant="contained"
              color="primary"
              data-cy="import_apply_import_button"
              onClick={onApply}
              loading={applyImportHelper.loading}
            >
              <T keyName="import_apply_button" />
            </LoadingButton>
          </Box>
        </Box>
      )}
      <ImportConflictNotResolvedErrorDialog
        onResolve={() => {
          resolveFirstUnresolved();
          applyImportHelper.onDialogClose();
        }}
        open={applyImportHelper.conflictNotResolvedDialogOpen}
        onClose={applyImportHelper.onDialogClose}
      />
    </BaseProjectView>
  );
};
