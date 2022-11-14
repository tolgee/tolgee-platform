import { MenuItem, Select, ListItemText } from '@mui/material';
import { Add } from '@mui/icons-material';
import { useTranslate } from '@tolgee/react';
import { useMemo, useState } from 'react';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';
import { NamespaceNewDialog } from './NamespaceNewDialog';

type Props = {
  value: string | undefined;
  onChange: (value: string | undefined) => void;
  namespaceData?: string[];
};

export const NamespaceSelector: React.FC<Props> = ({
  value,
  onChange,
  namespaceData,
}) => {
  const project = useProject();
  const t = useTranslate();

  const namespacesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/used-namespaces',
    method: 'get',
    path: { projectId: project.id },
    fetchOptions: {
      disableNotFoundHandling: true,
    },
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
  const [customOption, setCustomOption] = useState('');

  const existingOptions = useMemo(() => {
    let options = usedNamespaces;

    if (!options.includes('')) {
      options = ['', ...options];
    }

    if (!options.includes(customOption)) {
      options = [...options, customOption];
    }

    if (!options.includes(currentNamespace)) {
      options = [...options, currentNamespace];
    }

    return options.map((o) => ({
      value: o || '',
      name: o || t('namespace_select_default'),
    }));
  }, [usedNamespaces]);

  const handleClose = () => {
    setDialogOpen(false);
  };

  const handleConfirm = (value: string) => {
    setDialogOpen(false);
    setCustomOption(value);
    onChange(value);
  };

  return (
    <>
      <Select
        data-cy="namespaces-select"
        onChange={(e) => {
          const value = e.target.value;
          if (value !== undefined) {
            onChange(e.target.value);
          }
        }}
        renderValue={(value) => value || t('namespace_select_default')}
        displayEmpty
        value={currentNamespace}
        size="small"
        fullWidth
      >
        {existingOptions.map((o) => (
          <MenuItem
            key={o.value}
            value={o.value}
            data-cy="namespaces-select-option"
          >
            {o.name}
          </MenuItem>
        ))}
        <MenuItem
          onClick={() => setDialogOpen(true)}
          sx={{ display: 'flex', gap: 1 }}
          data-cy="namespaces-select-option-new"
        >
          <Add fontSize="small" />
          <ListItemText primary={t('namespace_select_new')} />
        </MenuItem>
      </Select>

      {dialogOpen && (
        <NamespaceNewDialog
          namespace={customOption}
          onChange={handleConfirm}
          onClose={handleClose}
        />
      )}
    </>
  );
};
