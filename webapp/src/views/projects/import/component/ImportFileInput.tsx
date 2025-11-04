import React, { FunctionComponent, ReactNode, useState } from 'react';
import { Box, Button, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { QuickStartHighlight } from 'tg.component/layout/QuickStartGuide/QuickStartHighlight';
import { DragDropArea } from 'tg.component/common/DragDropArea';
import { useConfig } from 'tg.globalContext/helpers';
import { ImportProgressOverlay } from './ImportProgressOverlay';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { FilesType } from 'tg.fixtures/FileUploadFixtures';

import {
  ImportInputAreaLayout,
  ImportInputAreaLayoutBottom,
  ImportInputAreaLayoutCenter,
  ImportInputAreaLayoutTitle,
  ImportInputAreaLayoutTop,
} from './ImportInputAreaLayout';
import { ImportSupportedFormats } from './ImportSupportedFormats';

export const MAX_FILE_COUNT = 20;

export type OperationType = 'addFiles' | 'apply';

export type OperationStatusType =
  | 'PREPARING_AND_VALIDATING'
  | 'STORING_KEYS'
  | 'STORING_TRANSLATIONS'
  | 'FINALIZING';

type ImportFileInputProps = {
  onNewFiles: (files: FilesType) => void;
  loading: boolean;
  operation?: OperationType;
  operationStatus?: OperationStatusType;
  importDone: boolean;
  onImportMore: () => void;
  filesUploaded?: boolean;
  onProgressOverlayActiveChange: (isActive: boolean) => void;
  isProgressOverlayActive: boolean;
};

export type ValidationResult = {
  valid: boolean;
  errors: ReactNode[];
};

const StyledRoot = styled(Box)(({ theme }) => ({
  borderRadius: theme.shape.borderRadius,
  border: `1px dashed ${theme.palette.tokens.border.secondary}`,
  margin: '0px auto',
  width: '100%',
  position: 'relative',
  backgroundColor: theme.palette.tokens.background['paper-3'],
  marginTop: '16px',
}));

const ImportFileInput: FunctionComponent<ImportFileInputProps> = (props) => {
  const { t } = useTranslate();
  const fileRef = React.createRef<HTMLInputElement>();
  const config = useConfig();
  const [resetKey, setResetKey] = useState(0);
  const { showMessage } = useGlobalActions();

  function resetInput() {
    setResetKey((key) => key + 1);
  }

  React.useEffect(() => {
    const listener = (e) => {
      e.preventDefault();
    };

    window.addEventListener('dragover', listener, false);
    window.addEventListener('drop', listener, false);

    return () => {
      window.removeEventListener('dragover', listener, false);
      window.removeEventListener('drop', listener, false);
    };
  }, []);

  function onFileSelected(e: React.SyntheticEvent) {
    const files = (e.target as HTMLInputElement).files;
    if (!files) {
      return;
    }
    const filtered: File[] = [];
    for (let i = 0; i < files.length; i++) {
      const item = files.item(i);
      if (item) {
        filtered.push(item);
      }
    }
    onNewFiles(filtered.map((f) => ({ file: f, name: f.name })));
  }

  const onNewFiles = (files: FilesType) => {
    resetInput();
    const validation = validate(files.map((f) => f.file));
    if (validation.valid) {
      props.onNewFiles(files);
      return;
    }
    validation.errors.forEach((e) =>
      showMessage({ text: e, variant: 'error' })
    );
  };

  const validate = (files: File[]): ValidationResult => {
    const result = {
      valid: false,
      errors: [] as ReactNode[],
    };

    if (files.length > MAX_FILE_COUNT) {
      result.errors.push(<T keyName="import_max_file_count_message" />);
    }

    files.forEach((file) => {
      if (file.size > config.maxUploadFileSize * 1024) {
        result.errors.push(
          <T
            keyName="translations.screenshots.validation.file_too_big"
            params={{ filename: file.name }}
          />
        );
      }
    });

    const valid = result.errors.length === 0;
    return { ...result, valid };
  };

  /* @ts-ignore */
  return (
    <DragDropArea
      onFilesReceived={onNewFiles}
      active={!props.isProgressOverlayActive}
      maxItems={MAX_FILE_COUNT}
    >
      <QuickStartHighlight
        offset={10}
        itemKey="pick_import_file"
        message={t('quick_start_item_pick_import_file_hint')}
      >
        <StyledRoot>
          <ImportInputAreaLayout>
            <ImportProgressOverlay
              operation={props.operation}
              importDone={props.importDone}
              loading={props.loading}
              onImportMore={props.onImportMore}
              filesUploaded={props.filesUploaded}
              operationStatus={props.operationStatus}
              onActiveChange={(isActive) =>
                props.onProgressOverlayActiveChange(isActive)
              }
            />
            <ImportInputAreaLayoutTop>
              <input
                key={resetKey}
                data-cy={'import-file-input'}
                type="file"
                style={{ display: 'none' }}
                ref={fileRef}
                onChange={(e) => onFileSelected(e)}
                multiple
                webkitdirectory
              />
              <ImportInputAreaLayoutTitle>
                <T keyName="import_file_input_drop_file_text" />
              </ImportInputAreaLayoutTitle>
            </ImportInputAreaLayoutTop>
            <ImportInputAreaLayoutCenter>
              <Button
                onClick={() =>
                  fileRef.current?.dispatchEvent(new MouseEvent('click'))
                }
                variant="outlined"
                color="primary"
              >
                <T keyName="import_file_input_select_file_button" />
              </Button>
            </ImportInputAreaLayoutCenter>
            <ImportInputAreaLayoutBottom>
              <ImportSupportedFormats />
            </ImportInputAreaLayoutBottom>
          </ImportInputAreaLayout>
        </StyledRoot>
      </QuickStartHighlight>
    </DragDropArea>
  );
};
export default ImportFileInput;
