import React, { FunctionComponent, ReactNode, useState } from 'react';
import { QuickStartHighlight } from 'tg.component/layout/QuickStartGuide/QuickStartHighlight';
import { Box, styled, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { container } from 'tsyringe';

import { useConfig } from 'tg.globalContext/helpers';
import { MessageActions } from 'tg.store/global/MessageActions';
import { Message } from 'tg.store/global/types';
import LoadingButton from 'tg.component/common/form/LoadingButton';

import { ImportFileDropzone } from './ImportFileDropzone';

export const MAX_FILE_COUNT = 20;

type ImportFileInputProps = {
  onNewFiles: (files: File[]) => void;
  loading: boolean;
};

export type ValidationResult = {
  valid: boolean;
  errors: ReactNode[];
};

const StyledRoot = styled(Box)(({ theme }) => ({
  borderRadius: theme.shape.borderRadius,
  border: `1px dashed ${theme.palette.emphasis[400]}`,
  maxWidth: 600,
  margin: '0px auto',
  width: '100%',
}));

const messageActions = container.resolve(MessageActions);
const ImportFileInput: FunctionComponent<ImportFileInputProps> = (props) => {
  const { t } = useTranslate();
  const fileRef = React.createRef<HTMLInputElement>();
  const config = useConfig();
  const ALLOWED_EXTENSIONS = ['json', 'zip', 'po', 'xliff', 'xlf'];
  const [resetKey, setResetKey] = useState(0);
  function resetInput() {
    setResetKey((key) => key + 1);
  }

  React.useEffect(() => {
    const listener = (e) => {
      e.preventDefault();
    };

    const pasteListener = (e: ClipboardEvent) => {
      const files: File[] = [];
      if (!e.clipboardData?.files.length) {
        return;
      }
      for (let i = 0; i < e.clipboardData.files.length; i++) {
        const item = e.clipboardData.files.item(i);
        if (item) {
          files.push(item);
        }
      }
      props.onNewFiles(files);
    };

    window.addEventListener('dragover', listener, false);
    window.addEventListener('drop', listener, false);
    document.addEventListener('paste', pasteListener);

    return () => {
      window.removeEventListener('dragover', listener, false);
      window.removeEventListener('drop', listener, false);
      document.removeEventListener('paste', pasteListener);
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
    onNewFiles(filtered);
  }

  const onNewFiles = (files: File[]) => {
    resetInput();
    const validation = validate(files);
    if (validation.valid) {
      props.onNewFiles(files);
      return;
    }
    validation.errors.forEach((e) =>
      messageActions.showMessage.dispatch(new Message(e, 'error'))
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
      const extension =
        file.name.indexOf('.') > -1 ? file.name.replace(/.*\.(.+)$/, '$1') : '';
      if (ALLOWED_EXTENSIONS.indexOf(extension) < 0) {
        result.errors.push(
          <T
            keyName="translations.screenshots.validation.unsupported_format"
            params={{ filename: file.name }}
          />
        );
      }
    });

    const valid = result.errors.length === 0;
    return { ...result, valid };
  };

  return (
    <ImportFileDropzone onNewFiles={onNewFiles}>
      <QuickStartHighlight
        offset={10}
        itemKey="pick_import_file"
        message={t('quick_start_item_pick_import_file_hint')}
      >
        <StyledRoot
          sx={{
            mt: 4,
            pt: 5,
            pb: 5,
            justifyContent: 'space-between',
            alignItems: 'center',
            flexDirection: 'column',
            display: 'flex',
          }}
        >
          <input
            key={resetKey}
            data-cy={'import-file-input'}
            type="file"
            style={{ display: 'none' }}
            ref={fileRef}
            onChange={(e) => onFileSelected(e)}
            multiple
            accept={ALLOWED_EXTENSIONS.join(',')}
          />
          <Typography variant="body1">
            <T keyName="import_file_input_drop_file_text" />
          </Typography>
          <Box mt={2} mb={2}>
            <LoadingButton
              loading={props.loading}
              onClick={() =>
                fileRef.current?.dispatchEvent(new MouseEvent('click'))
              }
              variant="outlined"
              color="primary"
            >
              <T keyName="import_file_input_select_file_button" />
            </LoadingButton>
          </Box>
          <Typography variant="body1">
            <T keyName="import_file_supported_formats" />
          </Typography>
        </StyledRoot>
      </QuickStartHighlight>
    </ImportFileDropzone>
  );
};

export default ImportFileInput;
