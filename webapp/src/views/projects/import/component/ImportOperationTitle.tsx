import { T } from '@tolgee/react';
import { OperationType } from './ImportFileInput';

export const ImportOperationTitle = (props: { operation: OperationType }) => {
  switch (props.operation) {
    case 'addFiles':
      return <T keyName="import-add-files-operation" />;
    case 'apply':
      return <T keyName="import-apply-operation" />;
  }
};
