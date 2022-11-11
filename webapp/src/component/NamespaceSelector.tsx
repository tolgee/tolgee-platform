import {
  MenuItem,
  TextField,
  Select,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  ListItemText,
} from '@mui/material';
import { Add } from '@mui/icons-material';
import { useTranslate } from '@tolgee/react';
import { useMemo, useState } from 'react';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';

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

  const handleConfirm = () => {
    setDialogOpen(false);
    onChange(customOption);
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
        fullWidth
        size="small"
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
        <Dialog open onClose={handleClose} fullWidth>
          <DialogTitle>{t('namespae_select_title')}</DialogTitle>

          <DialogContent>
            <TextField
              data-cy="namespaces-select-text-field"
              onChange={(e) => {
                setCustomOption(e.target.value);
              }}
              placeholder={t('namespace_select_placeholder')}
              value={customOption}
              fullWidth
              size="small"
              autoFocus
              onKeyPress={(e) => {
                if (e.key === 'Enter') {
                  handleConfirm();
                }
              }}
            />
          </DialogContent>
          <DialogActions>
            <Button data-cy="global-confirmation-cancel" onClick={handleClose}>
              {t('namespace_select_cancel')}
            </Button>
            <Button
              data-cy="global-confirmation-confirm"
              color="primary"
              onClick={handleConfirm}
            >
              {t('namespace_select_confirm')}
            </Button>
          </DialogActions>
        </Dialog>
      )}
    </>
  );
};
