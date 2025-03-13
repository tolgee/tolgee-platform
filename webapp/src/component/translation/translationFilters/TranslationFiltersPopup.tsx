import { Menu } from '@mui/material';
import { FiltersType } from './tools';
import { type FilterActions } from 'tg.views/projects/translations/context/services/useTranslationFilterService';
import { SubfilterTags } from './SubfilterTags';
import { SubfilterNamespaces } from './SubfilterNamespaces';
import { SubfilterTranslations } from './SubfilterTranslations';
import { SubfilterScreenshots } from './SubfilterScreenshots';

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
  return (
    <Menu
      open={true}
      anchorEl={anchorEl}
      onClose={() => onClose()}
      slotProps={{
        paper: {
          style: { minWidth: anchorEl.offsetWidth, maxWidth: 'unset' },
        },
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
      <SubfilterScreenshots
        value={value}
        actions={actions}
        projectId={projectId}
      />
    </Menu>
  );
};
