import { Box, Menu, MenuItem } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useProject } from 'tg.hooks/useProject';

import { FilterActions, FilterOptions } from './tools';
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
import { SubfilterLabels } from 'tg.views/projects/translations/TranslationFilters/SubfilterLabels';
import { SubfilterSuggestions } from './SubfilterSuggestions';

type Props = {
  value: FiltersType;
  actions: FilterActions;
  onClose: () => void;
  anchorEl: HTMLElement;
  projectId: number;
  selectedLanguages: LanguageModel[];
  showClearButton?: boolean;
  filterOptions?: FilterOptions;
};

export const TranslationFiltersPopup = ({
  value,
  actions,
  onClose,
  anchorEl,
  projectId,
  selectedLanguages,
  showClearButton,
  filterOptions,
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
      <Box display="grid">
        <SubfilterTags value={value} actions={actions} projectId={projectId} />
        {(project.useNamespaces ||
          Boolean(getNamespaceFiltersLength(value))) && (
          <SubfilterNamespaces
            value={value}
            actions={actions}
            projectId={projectId}
          />
        )}
        <SubfilterScreenshots
          value={value}
          actions={actions}
          projectId={projectId}
        />
        {!filterOptions?.keyRelatedOnly && (
          <>
            <SubfilterTranslations
              value={value}
              actions={actions}
              projectId={projectId}
              selectedLanguages={selectedLanguages}
            />
            <SubfilterLabels
              value={value}
              actions={actions}
              projectId={projectId}
              selectedLanguages={selectedLanguages}
            />
            <SubfilterComments
              value={value}
              actions={actions}
              projectId={projectId}
            />
          </>
        )}
        {project.suggestionsMode !== 'DISABLED' && (
          <SubfilterSuggestions
            value={value}
            actions={actions}
            projectId={projectId}
            selectedLanguages={selectedLanguages}
          />
        )}
        {showClearButton && Boolean(countFilters(value)) && (
          <MenuItem onClick={() => actions.setFilters({})}>
            {t('translations_filters_heading_clear')}
          </MenuItem>
        )}
      </Box>
    </Menu>
  );
};
