import { Menu, MenuItem } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { type FilterActions } from 'tg.views/projects/translations/context/services/useTranslationFilterService';

import { FiltersType, LanguageModel } from './tools';
import { countFilters } from './summary';
import { SubfilterTags } from './SubfilterTags';
import {
  getNamespaceFiltersLength,
  SubfilterNamespaces,
} from './SubfilterNamespaces';
import { SubfilterTranslations } from './SubfilterTranslations';
import { SubfilterScreenshots } from './SubfilterScreenshots';
import { SubfilterComments } from './SubfilterComments';
import { useProject } from 'tg.hooks/useProject';

type Props = {
  value: FiltersType;
  actions: FilterActions;
  onClose: () => void;
  anchorEl: HTMLElement;
  projectId: number;
  selectedLanguages: LanguageModel[];
  showClearButton?: boolean;
};

export const TranslationFiltersPopup = ({
  value,
  actions,
  onClose,
  anchorEl,
  projectId,
  selectedLanguages,
  showClearButton,
}: Props) => {
  const { t } = useTranslate();
  const project = useProject();
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
      {(project.useNamespaces || Boolean(getNamespaceFiltersLength(value))) && (
        <SubfilterNamespaces
          value={value}
          actions={actions}
          projectId={projectId}
        />
      )}
      <SubfilterTranslations
        value={value}
        actions={actions}
        projectId={projectId}
        selectedLanguages={selectedLanguages}
      />
      <SubfilterScreenshots
        value={value}
        actions={actions}
        projectId={projectId}
      />
      <SubfilterComments
        value={value}
        actions={actions}
        projectId={projectId}
      />
      {showClearButton && Boolean(countFilters(value)) && (
        <MenuItem onClick={() => actions.setFilters({})}>
          {t('translations_filters_heading_clear')}
        </MenuItem>
      )}
    </Menu>
  );
};
