import { Box, Menu, MenuItem } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { useProject } from 'tg.hooks/useProject';
import { useEnabledFeatures } from 'tg.globalContext/helpers';

import { FilterActions, FilterOptions } from './tools';
import { FiltersType, LanguageModel } from './tools';
import { countFilters, sameFilters } from './summary';
import { SubfilterTags } from './SubfilterTags';
import {
  getNamespaceFiltersLength,
  SubfilterNamespaces,
} from './SubfilterNamespaces';
import { SubfilterTranslations } from './SubfilterTranslations';
import { SubfilterScreenshots } from './SubfilterScreenshots';
import { SubfilterDescription } from './SubfilterDescription';
import { SubfilterComments } from './SubfilterComments';
import { SubfilterLabels } from 'tg.views/projects/translations/TranslationFilters/SubfilterLabels';
import { SubfilterSuggestions } from './SubfilterSuggestions';
import { SubfilterDeletedBy } from './SubfilterDeletedBy';
import {
  getQaChecksFiltersLength,
  SubfilterQaChecks,
  SubfilterTasks,
  getTaskFiltersLength,
} from 'tg.ee';

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
  const { isEnabled } = useEnabledFeatures();
  const tasksEnabled = isEnabled('TASKS') || isEnabled('ORDER_TRANSLATION');
  const clearedFilters = filterOptions?.clearedFilters ?? {};
  const tasksFilterAvailable =
    tasksEnabled || Boolean(getTaskFiltersLength(value));
  const surfaceShowsTaskFilter =
    !filterOptions?.keyRelatedOnly || filterOptions?.taskCreation;
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
        <SubfilterDescription
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
            {(project.useQaChecks ||
              Boolean(getQaChecksFiltersLength(value))) && (
              <SubfilterQaChecks
                value={value}
                actions={actions}
                selectedLanguages={selectedLanguages}
              />
            )}
          </>
        )}
        {tasksFilterAvailable && surfaceShowsTaskFilter && (
          <SubfilterTasks
            value={value}
            actions={actions}
            selectedLanguages={selectedLanguages}
            taskCreation={filterOptions?.taskCreation}
            pinnedTaskType={filterOptions?.pinnedTaskType}
          />
        )}
        {project.suggestionsMode !== 'DISABLED' && (
          <SubfilterSuggestions
            value={value}
            actions={actions}
            projectId={projectId}
            selectedLanguages={selectedLanguages}
          />
        )}
        {filterOptions?.showDeletedBy && (
          <SubfilterDeletedBy
            value={value}
            actions={actions}
            projectId={projectId}
          />
        )}
        {showClearButton &&
          Boolean(countFilters(value)) &&
          !sameFilters(value, clearedFilters) && (
            <MenuItem onClick={() => actions.setFilters(clearedFilters)}>
              {t('translations_filters_heading_clear')}
            </MenuItem>
          )}
      </Box>
    </Menu>
  );
};
