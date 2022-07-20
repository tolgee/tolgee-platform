import { FunctionComponent, useEffect, useState } from 'react';
import { Box, Button } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { container } from 'tsyringe';

import { LINKS, PARAMS } from 'tg.constants/links';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { confirmation } from 'tg.hooks/confirmation';
import { useProject } from 'tg.hooks/useProject';
import { MessageService } from 'tg.service/MessageService';
import { components } from 'tg.service/apiSchema.generated';
import { ImportActions } from 'tg.store/project/ImportActions';

import { ImportAlertError } from './ImportAlertError';
import { ImportConflictNotResolvedErrorDialog } from './component/ImportConflictNotResolvedErrorDialog';
import { ImportConflictResolutionDialog } from './component/ImportConflictResolutionDialog';
import ImportFileInput from './component/ImportFileInput';
import { ImportResult } from './component/ImportResult';
import { useApplyImportHelper } from './hooks/useApplyImportHelper';
import { useImportDataHelper } from './hooks/useImportDataHelper';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { BaseProjectView } from '../BaseProjectView';
import { useOrganizationUsageMethods } from 'tg.globalContext/helpers';

const actions = container.resolve(ImportActions);
const messageService = container.resolve(MessageService);

export const ImportView: FunctionComponent = () => {
  const dataHelper = useImportDataHelper();
  const project = useProject();
  const applyImportHelper = useApplyImportHelper(dataHelper);
  const cancelLoadable = actions.useSelector((s) => s.loadables.cancelImport);
  const deleteLanguageLoadable = actions.useSelector(
    (s) => s.loadables.deleteLanguage
  );
  const addFilesLoadable = actions.useSelector((s) => s.loadables.addFiles);
  const resultLoadable = actions.useSelector((s) => s.loadables.getResult);
  const resultLoading = resultLoadable.loading || addFilesLoadable.loading;
  const selectLanguageLoadable = actions.useSelector(
    (s) => s.loadables.selectLanguage
  );
  const resetExistingLanguageLoadable = actions.useSelector(
    (s) => s.loadables.resetExistingLanguage
  );
  const [resolveRow, setResolveRow] = useState(
    undefined as components['schemas']['ImportLanguageModel'] | undefined
  );
  const { refetchUsage } = useOrganizationUsageMethods();

  const t = useTranslate();

  const onConflictResolutionDialogClose = () => {
    dataHelper.loadData();
    setResolveRow(undefined);
  };

  const resolveFirstUnresolved = () => {
    const row = dataHelper.result?._embedded?.languages?.find(
      (l) => l.conflictCount > l.resolvedCount
    );
    setResolveRow(row);
  };

  useGlobalLoading(
    (deleteLanguageLoadable.loading && deleteLanguageLoadable.loaded) ||
      (selectLanguageLoadable.loading && selectLanguageLoadable.loaded) ||
      (resetExistingLanguageLoadable.loading &&
        resetExistingLanguageLoadable.loaded) ||
      resultLoadable.loading
  );

  useEffect(() => {
    const error = resultLoadable.error;
    if (error?.code === 'resource_not_found') {
      dataHelper.resetResult();
    }
  }, [resultLoadable.loading, addFilesLoadable.loading]);

  useEffect(() => {
    if (
      (deleteLanguageLoadable.loaded && !deleteLanguageLoadable.loading) ||
      (selectLanguageLoadable.loaded && !selectLanguageLoadable.loading) ||
      (resetExistingLanguageLoadable.loaded &&
        !resetExistingLanguageLoadable.loading)
    ) {
      dataHelper.loadData();
    }
  }, [
    deleteLanguageLoadable.loading,
    selectLanguageLoadable.loading,
    resetExistingLanguageLoadable.loading,
  ]);

  useEffect(() => {
    if (!resultLoading) {
      dataHelper.loadData();
    }
  }, []);

  useEffect(() => {
    if (cancelLoadable.loaded) {
      dataHelper.resetResult();
    }
  }, [cancelLoadable.loading]);

  useEffect(() => {
    if (applyImportHelper.error) {
      const parsed = parseErrorResponse(applyImportHelper.error);
      messageService.error(<T>{parsed[0]}</T>);
      actions.loadableReset.applyImport.dispatch();
    }
  }, [applyImportHelper.error]);

  const onApply = () => {
    actions.touchApply.dispatch();
    if (dataHelper.isValid) {
      applyImportHelper.onApplyImport();
    }
  };

  useEffect(() => {
    if (addFilesLoadable.error?.code === 'cannot_add_more_then_100_languages') {
      messageService.error(
        <T parameters={{ n: '100' }}>
          import_error_cannot_add_more_then_n_languages
        </T>
      );
    }
  }, [addFilesLoadable.error?.code]);

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
      lg={7}
      md={9}
      containerMaxWidth="lg"
    >
      <ImportConflictResolutionDialog
        row={resolveRow}
        onClose={onConflictResolutionDialogClose}
      />
      <Box mt={2}>
        <ImportFileInput
          onNewFiles={dataHelper.onNewFiles}
          loading={addFilesLoadable.loading}
        />

        {addFilesLoadable.data?.errors?.map((e, idx) => (
          <ImportAlertError key={idx} error={e} />
        ))}
        <ImportResult
          onResolveRow={setResolveRow}
          onLoadData={dataHelper.loadData}
          result={dataHelper.result}
        />
      </Box>
      {dataHelper.result && (
        <Box display="flex" mt={2} justifyContent="flex-end">
          <Box mr={2}>
            <Button
              data-cy="import_cancel_import_button"
              variant="outlined"
              color="primary"
              onClick={() => {
                confirmation({
                  onConfirm: () =>
                    actions.loadableActions.cancelImport.dispatch({
                      path: {
                        projectId: project.id,
                      },
                    }),
                  title: <T>import_cancel_confirmation_title</T>,
                  message: <T>import_cancel_confirmation_message</T>,
                });
              }}
            >
              <T>import_cancel_button</T>
            </Button>
          </Box>
          <Box>
            <LoadingButton
              variant="contained"
              color="primary"
              data-cy="import_apply_import_button"
              onClick={onApply}
              loading={applyImportHelper.loading}
            >
              <T>import_apply_button</T>
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
