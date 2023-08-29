import { useTranslate } from '@tolgee/react';
import { ComponentProps, useMemo, useState } from 'react';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { NamespaceNewDialog } from './NamespaceNewDialog';
import { SearchSelect } from 'tg.component/searchSelect/SearchSelect';

type Props = {
  value: string | undefined;
  onChange: (value: string | undefined) => void;
  namespaceData?: string[];
  SearchSelectProps?: Partial<ComponentProps<typeof SearchSelect>>;
};

export const NamespaceSelector: React.FC<Props> = ({
  value,
  onChange,
  namespaceData,
  SearchSelectProps,
}) => {
  const project = useProject();
  const { t } = useTranslate();
  const [lastSearch, setLastSearch] = useState('');

  const namespacesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/used-namespaces',
    method: 'get',
    path: { projectId: project.id },
    options: {
      enabled: !namespaceData,
    },
  });

  const currentNamespace = value || '';

  const usedNamespaces = useMemo(() => {
    return (
      namespaceData ||
      namespacesLoadable?.data?._embedded?.namespaces?.map((ns) => ns.name) ||
      []
    ).map((v) => v || '');
  }, [namespacesLoadable.data, namespaceData]);

  const [dialogOpen, setDialogOpen] = useState(false);

  const existingOptions = useMemo(() => {
    let options = [...usedNamespaces].sort();

    if (!options.includes(currentNamespace)) {
      options = [currentNamespace, ...options];
    }

    if (!options.includes('')) {
      options = ['', ...options];
    }

    return options.map((o) => ({
      value: o || '',
      name: o || t('namespace_select_default'),
    }));
  }, [usedNamespaces, currentNamespace]);

  const handleClose = () => {
    setDialogOpen(false);
  };

  const handleConfirm = (value: string) => {
    setDialogOpen(false);
    onChange(value);
  };

  return (
    <div data-cy="namespaces-selector">
      <SearchSelect
        {...SearchSelectProps}
        onAddNew={(searchValue) => {
          setLastSearch(searchValue);
          setDialogOpen(true);
        }}
        searchPlaceholder={t('namespace_select_search')}
        addNewTooltip={t('namespace_select_new')}
        onSelect={onChange}
        items={existingOptions}
        value={value || ''}
        SelectProps={{ size: 'small', ...SearchSelectProps?.SelectProps }}
      />
      {Boolean(dialogOpen) && (
        <NamespaceNewDialog
          namespace={lastSearch}
          onChange={handleConfirm}
          onClose={handleClose}
        />
      )}
    </div>
  );
};
