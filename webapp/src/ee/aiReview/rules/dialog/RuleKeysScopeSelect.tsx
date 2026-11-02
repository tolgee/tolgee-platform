import { useRef, useState } from 'react';
import { Box, Menu, TextField } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { useProject } from 'tg.hooks/useProject';
import { SubfilterTags } from 'tg.views/projects/translations/TranslationFilters/SubfilterTags';
import {
  getNamespaceFiltersLength,
  SubfilterNamespaces,
} from 'tg.views/projects/translations/TranslationFilters/SubfilterNamespaces';
import {
  AddParams,
  FilterActions,
  FiltersInternal,
} from 'tg.views/projects/translations/TranslationFilters/tools';

export type RuleKeysScope = {
  tagIn: string[];
  tagNotIn: string[];
  namespaceIn: string[];
  namespaceNotIn: string[];
};

type Props = {
  value: RuleKeysScope;
  onChange: (value: RuleKeysScope) => void;
};

const FILTER_TO_SCOPE = {
  filterTag: 'tagIn',
  filterNoTag: 'tagNotIn',
  filterNamespace: 'namespaceIn',
  filterNoNamespace: 'namespaceNotIn',
} as const;

export const RuleKeysScopeSelect = ({ value, onChange }: Props) => {
  const { t } = useTranslate();
  const project = useProject();
  const anchorEl = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);

  const filters: FiltersInternal = {
    filterTag: value.tagIn,
    filterNoTag: value.tagNotIn,
    filterNamespace: value.namespaceIn,
    filterNoNamespace: value.namespaceNotIn,
  };

  const actions: FilterActions = {
    addFilter(...params: AddParams) {
      const [filter, item] = params;
      const scopeField = FILTER_TO_SCOPE[filter];
      if (scopeField === undefined || item === undefined) {
        return;
      }
      onChange({ ...value, [scopeField]: [...value[scopeField], item] });
    },
    removeFilter(...params: AddParams) {
      const [filter, item] = params;
      const scopeField = FILTER_TO_SCOPE[filter];
      if (scopeField === undefined) {
        return;
      }
      onChange({
        ...value,
        [scopeField]: value[scopeField].filter((existing) => existing !== item),
      });
    },
    setFilters() {
      onChange({
        tagIn: [],
        tagNotIn: [],
        namespaceIn: [],
        namespaceNotIn: [],
      });
    },
  };

  const count =
    value.tagIn.length +
    value.tagNotIn.length +
    value.namespaceIn.length +
    value.namespaceNotIn.length;

  return (
    <>
      <TextField
        variant="outlined"
        size="small"
        label={t('ai_review_rule_form_keys_label', 'Keys')}
        value={
          count > 0
            ? t(
                'ai_review_rule_form_keys_filters',
                '{count, plural, one {# filter} other {# filters}}',
                {
                  count,
                }
              )
            : t('ai_review_rule_form_keys_all', 'All keys')
        }
        onClick={() => setOpen(true)}
        ref={anchorEl}
        InputProps={{
          readOnly: true,
          sx: { cursor: 'pointer' },
        }}
        data-cy="ai-review-rule-form-keys-select"
      />
      {open && (
        <Menu
          open={true}
          anchorEl={anchorEl.current}
          onClose={() => setOpen(false)}
          slotProps={{
            paper: {
              style: { minWidth: anchorEl.current?.offsetWidth },
            },
          }}
        >
          <Box display="grid">
            <SubfilterTags
              value={filters}
              actions={actions}
              projectId={project.id}
            />
            {(project.useNamespaces ||
              Boolean(getNamespaceFiltersLength(filters))) && (
              <SubfilterNamespaces
                value={filters}
                actions={actions}
                projectId={project.id}
              />
            )}
          </Box>
        </Menu>
      )}
    </>
  );
};
