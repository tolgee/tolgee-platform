import {
  default as React,
  FunctionComponent,
  useEffect,
  useState,
} from 'react';
import { Box, Button } from '@material-ui/core';
import { LINKS, PARAMS } from '../../../constants/links';
import { Navigation } from '../../../component/navigation/Navigation';
import { BaseView } from '../../../component/layout/BaseView';
import { T, useTranslate } from '@tolgee/react';
import ImportFileInput from './component/ImportFileInput';
import { useImportDataHelper } from './hooks/useImportDataHelper';
import { ImportResult } from './component/ImportResult';
import { container } from 'tsyringe';
import { ImportActions } from '../../../store/project/ImportActions';
import { useProject } from '../../../hooks/useProject';
import { ImportConflictNotResolvedErrorDialog } from './component/ImportConflictNotResolvedErrorDialog';
import { useApplyImportHelper } from './hooks/useApplyImportHelper';
import { startLoading, stopLoading } from '../../../hooks/loading';
import { parseErrorResponse } from '../../../fixtures/errorFIxtures';
import { MessageService } from '../../../service/MessageService';
import { ImportAlertError } from './ImportAlertError';
import { confirmation } from '../../../hooks/confirmation';
import { components } from '../../../service/apiSchema.generated';
import { ImportConflictResolutionDialog } from './component/ImportConflictResolutionDialog';

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

  useEffect(() => {
    if (!resultLoading) {
      stopLoading();
    }

    const error = resultLoadable.error;
    if (error?.code === 'resource_not_found') {
      dataHelper.resetResult();
    }
  }, [resultLoadable.loading, addFilesLoadable.loading]);

  useEffect(() => {
    startLoading();
    if (
      (deleteLanguageLoadable.loaded && !deleteLanguageLoadable.loading) ||
      (selectLanguageLoadable.loaded && !selectLanguageLoadable.loading) ||
      (resetExistingLanguageLoadable.loaded &&
        !resetExistingLanguageLoadable.loading)
    ) {
      stopLoading();
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

  if (addFilesLoadable.error?.code === 'cannot_add_more_then_100_languages') {
    messageService.error(
      <T parameters={{ n: '100' }}>
        import_error_cannot_add_more_then_n_languages
      </T>
    );
  }

  return (
    <BaseView
      navigation={
        <Navigation
          path={[
            [
              project.name,
              LINKS.PROJECT_TRANSLATIONS.build({
                [PARAMS.PROJECT_ID]: project.id,
              }),
            ],
            [
              t('import_translations_title'),
              LINKS.PROJECT_IMPORT.build({
                [PARAMS.PROJECT_ID]: project.id,
              }),
            ],
          ]}
        />
      }
      xs={12}
      md={10}
      lg={8}
    >
      <ImportConflictResolutionDialog
        row={resolveRow}
        onClose={onConflictResolutionDialogClose}
      />
      <Box mt={2}>
        <ImportFileInput onNewFiles={dataHelper.onNewFiles} />

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
            <Button
              variant="contained"
              color="primary"
              data-cy="import_apply_import_button"
              onClick={onApply}
            >
              <T>import_apply_button</T>
            </Button>
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
    </BaseView>
  );
};
