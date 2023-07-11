import { useTranslate } from '@tolgee/react';
import { SearchSelect } from 'tg.component/searchSelect/SearchSelect';
import { BatchActions, OperationProps } from './types';

type Props = OperationProps & {
  value: BatchActions;
  onChange: (value: BatchActions) => void;
};

export const BatchSelect = ({ value, onChange, disabled }: Props) => {
  const { t } = useTranslate();
  return (
    <SearchSelect
      value={value}
      items={[
        { value: 'delete', name: t('batch_operations_delete') },
        { value: 'translate', name: t('batch_operations_translate') },
      ]}
      onChange={onChange}
      SelectProps={{ size: 'small', disabled }}
      noContain
      displaySearch={false}
    />
  );
};
