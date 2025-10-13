import React, { FunctionComponent, useEffect, useState } from 'react';
import { Box, Button } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { LINKS, PARAMS } from 'tg.constants/links';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { confirmation } from 'tg.hooks/confirmation';
import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import { messageService } from 'tg.service/MessageService';

import { ImportAlertError } from './component/ImportAlertError';
import { ImportConflictNotResolvedErrorDialog } from './component/ImportConflictNotResolvedErrorDialog';
import { ImportConflictResolutionDialog } from './component/ImportConflictResolutionDialog';
import ImportFileInput from './component/ImportFileInput';
import { ImportResult } from './component/ImportResult';
import { useApplyImportHelper } from './hooks/useApplyImportHelper';
import { useImportDataHelper } from './hooks/useImportDataHelper';
import { BaseProjectView } from '../BaseProjectView';
import { ImportResultLoadingOverlay } from './component/ImportResultLoadingOverlay';
import { ImportSettingsPanel } from './component/ImportSettingsPanel';
import { TranslatedWarningBox } from 'tg.translationTools/TranslatedWarningBox';
import { useOnFilePaste } from 'tg.fixtures/useOnFilePaste';

export const ImportView: FunctionComponent = () => {
  const dataHelper = useImportDataHelper();
  useOnFilePaste(dataHelper.onNewFiles);

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

  const loading =
    dataHelper.addFilesMutation.isLoading || applyImportHelper.loading;

  const [isProgressOverlayActive, setIsProgressOverlayActive] = useState(false);

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
      overflow="auto"
    >
      <ImportConflictResolutionDialog
        row={resolveRow}
        onClose={onConflictResolutionDialogClose}
      />
      <Box mt={2} position="relative">
        <ImportFileInput
          onNewFiles={dataHelper.onNewFiles}
          loading={loading}
          operationStatus={applyImportHelper.status}
          importDone={applyImportHelper.loaded}
          operation={
            applyImportHelper.loading
              ? 'apply'
              : dataHelper.addFilesMutation.isLoading
              ? 'addFiles'
              : undefined
          }
          onImportMore={() => {
            applyImportHelper.clear();
            dataHelper.addFilesMutation.reset();
          }}
          filesUploaded={dataHelper.addFilesMutation.isSuccess}
          isProgressOverlayActive={isProgressOverlayActive}
          onProgressOverlayActiveChange={(isActive) =>
            setIsProgressOverlayActive(isActive)
          }
        />

        {dataHelper.addFilesMutation.data?.errors?.map((e, idx) => (
          <ImportAlertError
            key={idx}
            error={e}
            addFilesMutation={dataHelper.addFilesMutation}
          />
        ))}
        {dataHelper.addFilesMutation.data?.warnings.map((item) => (
          <Box key={item.code} mt={4} data-cy="import-file-warnings">
            <TranslatedWarningBox code={item.code} />
          </Box>
        ))}
        <Box position="relative">
          <ImportResultLoadingOverlay loading={isProgressOverlayActive} />
          <ImportSettingsPanel />

          <ImportResult
            onResolveRow={setResolveRow}
            onLoadData={dataHelper.refetchData}
            result={dataHelper.result}
          />
          {dataHelper.result && (
            <Box display="flex" mt={2} justifyContent="flex-end">
              <Box mr={2}>
                <Button
                  data-cy="import_cancel_import_button"
                  variant="outlined"
                  color="primary"
                  onClick={() => {
                    confirmation({
                      onConfirm: () => dataHelper.onCancel(),
                      title: <T keyName="import_cancel_confirmation_title" />,
                      message: (
                        <T keyName="import_cancel_confirmation_message" />
                      ),
                    });
                  }}
                >
                  <T keyName="import_cancel_button" />
                </Button>
              </Box>
              <Box>
                <Button
                  variant="contained"
                  color="primary"
                  data-cy="import_apply_import_button"
                  onClick={onApply}
                >
                  <T keyName="import_apply_button" />
                </Button>
              </Box>
            </Box>
          )}
        </Box>
      </Box>
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
