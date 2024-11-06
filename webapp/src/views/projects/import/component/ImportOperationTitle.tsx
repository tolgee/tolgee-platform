import { T } from '@tolgee/react';
import { OperationType } from './ImportFileInput';
import { Tada } from 'tg.component/CustomIcons';
import { Box } from '@mui/material';
import { ImportInputAreaLayoutTitle } from './ImportInputAreaLayout';

export const ImportOperationTitle = (props: {
  operation?: OperationType;
  filesUploaded?: boolean;
  importDone?: boolean;
}) => {
  const Message = () => {
    if (props.importDone) {
      return (
        <ImportInputAreaLayoutTitle
          icon={<Tada style={{ marginLeft: '8px' }} />}
        >
          <T keyName="import-data-imported-info" />
        </ImportInputAreaLayoutTitle>
      );
    }

    if (props.filesUploaded) {
      return (
        <ImportInputAreaLayoutTitle>
          <T keyName="import_files_uploaded" />
        </ImportInputAreaLayoutTitle>
      );
    }
    switch (props.operation) {
      case 'addFiles':
        return (
          <ImportInputAreaLayoutTitle>
            <T keyName="import-add-files-operation" />
          </ImportInputAreaLayoutTitle>
        );
      case 'apply':
        return (
          <ImportInputAreaLayoutTitle>
            <T keyName="import-apply-operation" />
          </ImportInputAreaLayoutTitle>
        );
    }

    return null;
  };

  return (
    <Box display="flex" alignItems="center">
      <Message />
    </Box>
  );
};
