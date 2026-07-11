import { T } from '@tolgee/react';
import { OperationStatusType } from './ImportFileInput';
import { useDebounce } from 'use-debounce';
import React from 'react';

export const ImportOperationStatus = (props: {
  status?: OperationStatusType;
  importedKeys?: number | null;
  totalKeys?: number | null;
}) => {
  const [debouncedStatus] = useDebounce(props.status, 100, { leading: true });

  const keysProgress =
    props.importedKeys != null && props.totalKeys != null
      ? ` (${props.importedKeys.toLocaleString()} / ${props.totalKeys.toLocaleString()})`
      : '';

  switch (debouncedStatus) {
    case 'PREPARING_AND_VALIDATING':
      return <T keyName="import-status-preparing-and-validating" />;
    case 'STORING_KEYS':
      return <T keyName="import-status-storing-keys" />;
    case 'STORING_TRANSLATIONS':
      return (
        <>
          <T keyName="import-status-storing-translations" />
          {keysProgress}
        </>
      );
    case 'FINALIZING':
      return <T keyName="import-status-finalizing" />;
    default:
      return null;
  }
};
