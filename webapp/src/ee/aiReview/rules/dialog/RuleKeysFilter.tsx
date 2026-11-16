import { useRef, useState } from 'react';
import { Box, IconButton, Menu, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { XClose } from '@untitled-ui/icons-react';
import { useFormikContext } from 'formik';

import { ArrowDropDown } from 'tg.component/CustomIcons';
import { FakeInput } from 'tg.component/FakeInput';
import { FieldLabel } from 'tg.component/FormField';
import { TextField } from 'tg.component/common/TextField';
import { stopBubble } from 'tg.fixtures/eventHandler';
import { useProject } from 'tg.hooks/useProject';
import { RuleFormValues } from 'tg.ee.module/aiReview/rules/dialog/useRuleFormValues';
import {
  getNamespaceFiltersLength,
  getNamespaceFiltersName,
  SubfilterNamespaces,
} from 'tg.views/projects/translations/TranslationFilters/SubfilterNamespaces';
import {
  getTagFiltersLength,
  getTagFiltersName,
  SubfilterTags,
} from 'tg.views/projects/translations/TranslationFilters/SubfilterTags';
import {
  AddParams,
  FilterActions,
  FiltersInternal,
} from 'tg.views/projects/translations/TranslationFilters/tools';

const StyledInputButton = styled(IconButton)`
  margin: ${({ theme }) => theme.spacing(-1, -0.5, -1, -0.25)};
`;

type ScopeField = 'tagIn' | 'tagNotIn' | 'namespaceIn' | 'namespaceNotIn';

const SCOPE_FIELDS: Partial<
  Record<AddParams[0], { field: ScopeField; opposite: ScopeField }>
> = {
  filterTag: { field: 'tagIn', opposite: 'tagNotIn' },
  filterNoTag: { field: 'tagNotIn', opposite: 'tagIn' },
  filterNamespace: { field: 'namespaceIn', opposite: 'namespaceNotIn' },
  filterNoNamespace: { field: 'namespaceNotIn', opposite: 'namespaceIn' },
};

export const RuleKeysFilter = () => {
  const { t } = useTranslate();
  const project = useProject();
  const { values, setFieldValue } = useFormikContext<RuleFormValues>();
  const anchorEl = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);

  const filters: FiltersInternal = {
    filterTag: values.tagIn,
    filterNoTag: values.tagNotIn,
    filterNamespace: values.namespaceIn,
    filterNoNamespace: values.namespaceNotIn,
  };

  const actions: FilterActions = {
    addFilter(...[name, item]: AddParams) {
      const scope = SCOPE_FIELDS[name];
      if (!scope) {
        return;
      }
      const added = String(item);
      setFieldValue(scope.field, [...values[scope.field], added]);
      setFieldValue(
        scope.opposite,
        values[scope.opposite].filter((value) => value !== added)
      );
    },
    removeFilter(...[name, item]: AddParams) {
      const scope = SCOPE_FIELDS[name];
      if (!scope) {
        return;
      }
      setFieldValue(
        scope.field,
        values[scope.field].filter((value) => value !== String(item))
      );
    },
    setFilters() {
      setFieldValue('tagIn', []);
      setFieldValue('tagNotIn', []);
      setFieldValue('namespaceIn', []);
      setFieldValue('namespaceNotIn', []);
    },
  };

  const numberOfFilters =
    getTagFiltersLength(filters) + getNamespaceFiltersLength(filters);

  function summary() {
    if (numberOfFilters === 0) {
      return t('ai_review_rule_form_filter_all_keys', 'All keys');
    }
    if (numberOfFilters === 1) {
      return getNamespaceFiltersName(filters) || getTagFiltersName(filters);
    }
    return t(
      'ai_review_rule_form_filter_count',
      '{count, plural, one {# filter} other {# filters}}',
      { count: numberOfFilters }
    );
  }

  return (
    <Box display="grid" gap={0.5} data-cy="ai-review-rule-form-keys-filter">
      <FieldLabel>
        {t('ai_review_rule_form_filter_keys_label', 'Filter keys')}
      </FieldLabel>
      <TextField
        variant="outlined"
        data-cy="ai-review-rule-form-keys-filter-select"
        minHeight={false}
        InputProps={{
          onClick: () => setOpen(true),
          ref: anchorEl,
          fullWidth: true,
          sx: { cursor: 'pointer' },
          readOnly: true,
          inputComponent: FakeInput,
          inputProps: {
            style: { contain: 'size', display: 'flex', alignItems: 'center' },
          },
          margin: 'dense',
          value: (
            <Box
              display="block"
              overflow="hidden"
              textOverflow="ellipsis"
              whiteSpace="nowrap"
            >
              {summary()}
            </Box>
          ),
          endAdornment: (
            <Box
              sx={{ display: 'flex', marginRight: -0.5, alignItems: 'center' }}
            >
              {numberOfFilters > 0 && (
                <StyledInputButton
                  size="small"
                  onClick={stopBubble(() => actions.setFilters({}))}
                  tabIndex={-1}
                  data-cy="ai-review-rule-form-keys-filter-clear"
                >
                  <XClose width={20} height={20} />
                </StyledInputButton>
              )}
              <StyledInputButton
                size="small"
                tabIndex={-1}
                sx={{ pointerEvents: 'none' }}
              >
                <ArrowDropDown />
              </StyledInputButton>
            </Box>
          ),
        }}
      />
      {open && (
        <Menu
          open={true}
          anchorEl={anchorEl.current}
          onClose={() => setOpen(false)}
          slotProps={{
            paper: {
              style: {
                minWidth: anchorEl.current?.offsetWidth,
                maxWidth: 'unset',
              },
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
    </Box>
  );
};
