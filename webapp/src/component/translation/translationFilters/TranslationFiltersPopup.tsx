import { Menu } from '@mui/material';
import { FiltersType } from './tools';
import { useTranslate } from '@tolgee/react';
import { SubfilterTags } from './SubfilterTags';
import { type FilterActions } from 'tg.views/projects/translations/context/services/useTranslationFilterService';
import { SubfilterNamespaces } from './SubfilterNamespaces';
import { SubfilterTranslations } from './SubfilterTranslations';

type Props = {
  value: FiltersType;
  actions: FilterActions;
  onClose: () => void;
  anchorEl: HTMLElement;
  projectId: number;
};

export const TranslationFiltersPopup = ({
  value,
  actions,
  onClose,
  anchorEl,
  projectId,
}: Props) => {
  const { t } = useTranslate();
  return (
    <Menu
      open={true}
      anchorEl={anchorEl}
      onClose={() => onClose()}
      slotProps={{
        paper: { style: { minWidth: anchorEl.offsetWidth, maxWidth: 'unset' } },
      }}
    >
      <SubfilterTags value={value} actions={actions} projectId={projectId} />
      <SubfilterNamespaces
        value={value}
        actions={actions}
        projectId={projectId}
      />
      <SubfilterTranslations
        value={value}
        actions={actions}
        projectId={projectId}
      />
    </Menu>
  );
};
