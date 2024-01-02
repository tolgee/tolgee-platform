import { T } from '@tolgee/react';
import { OperationStatusType } from './ImportFileInput';
import { useDebounce } from 'use-debounce';
import React from 'react';

export const ImportOperationStatus = (props: {
  status?: OperationStatusType;
}) => {
  const [debouncedStatus] = useDebounce(props.status, 1000, { leading: true });

  switch (debouncedStatus) {
    case 'PREPARING_AND_VALIDATING':
      return <T keyName="import-status-preparing-and-validating" />;
    case 'STORING_KEYS':
      return <T keyName="import-status-storing-keys" />;
    case 'STORING_TRANSLATIONS':
      return <T keyName="import-status-storing-translations" />;
    case 'FINALIZING':
      return <T keyName="import-status-finalizing" />;
  }
  return null;
};
