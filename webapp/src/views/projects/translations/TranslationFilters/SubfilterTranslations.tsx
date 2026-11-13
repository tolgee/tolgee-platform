import { ReactNode, useRef, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { Box, Menu, MenuItem } from '@mui/material';
import { ChevronDown, ChevronUp } from '@untitled-ui/icons-react';

import { SubmenuItem } from 'tg.component/SubmenuItem';
import { useStateTranslation } from 'tg.translationTools/useStateTranslation';
import { CompactListSubheader } from 'tg.component/ListComponents';
import { Mt, TranslationMemory } from 'tg.component/CustomIcons';
import { exhaustiveMatchingGuard } from 'tg.fixtures/exhaustiveMatchingGuard';
import {
  AutoTranslatedBy,
  FiltersInternal,
  FilterActions,
  TranslationStateType,
} from 'tg.views/projects/translations/TranslationFilters/tools';
import {
  StateIndicator,
  StyledIcon,
} from 'tg.views/projects/translations/TranslationFilters/StateIndicator';
import {
  AUTO_TRANSLATED_SOURCES,
  selectedAutoTranslatedSources,
  toggleAutoTranslated,
  toggleAutoTranslatedBy,
} from 'tg.views/projects/translations/TranslationFilters/useTranslationFilters';

import { FilterItem } from './FilterItem';
import { LanguageModel } from './tools';

type Props = {
  projectId: number;
  value: FiltersInternal;
  actions: FilterActions;
  selectedLanguages: LanguageModel[];
};

const states: TranslationStateType[] = [
  'UNTRANSLATED',
  'TRANSLATED',
  'REVIEWED',
  'AUTO_TRANSLATED',
  'OUTDATED',
  'DISABLED',
];

export const SubfilterTranslations = ({
  value,
  actions,
  selectedLanguages,
}: Props) => {
  const { t } = useTranslate();
  const [open, setOpen] = useState(false);
  const anchorEl = useRef<HTMLElement>(null);
  const translateState = useStateTranslation();
  const [expanded, setExpanded] = useState(
    value.filterTranslationLanguage !== undefined
  );
  const autoSources = selectedAutoTranslatedSources(value);

  function toggleFilterLanguage(
    newValue: FiltersInternal['filterTranslationLanguage']
  ) {
    actions.setFilters({
      ...value,
      filterTranslationLanguage:
        newValue === value.filterTranslationLanguage ? undefined : newValue,
    });
  }

  function toggleFilterState(newValue: TranslationStateType) {
    if (value.filterTranslationState?.includes(newValue)) {
      actions.removeFilter('filterTranslationState', newValue);
    } else {
      actions.addFilter('filterTranslationState', newValue);
    }
  }

  return (
    <>
      <SubmenuItem
        ref={anchorEl as any}
        label={t('translations_filters_heading_translations')}
        onClick={() => setOpen(true)}
        selected={Boolean(getTranslationFiltersLength(value))}
        open={open}
      />
      {open && (
        <Menu
          open={open}
          anchorEl={anchorEl.current!}
          anchorOrigin={{
            vertical: 'top',
            horizontal: 'right',
          }}
          transformOrigin={{
            vertical: 'top',
            horizontal: 'left',
          }}
          onClose={() => {
            setOpen(false);
          }}
          slotProps={{ paper: { style: { minWidth: 250 } } }}
        >
          <Box display="grid">
            {states.map((state) => {
              if (state !== 'AUTO_TRANSLATED') {
                return (
                  <FilterItem
                    key={state}
                    label={translateState(state)}
                    selected={Boolean(
                      value.filterTranslationState?.includes(state)
                    )}
                    onClick={() => toggleFilterState(state)}
                    indicator={<StateIndicator state={state} />}
                  />
                );
              }
              return [
                <FilterItem
                  key={state}
                  label={translateState(state)}
                  selected={
                    autoSources.length === AUTO_TRANSLATED_SOURCES.length
                  }
                  indeterminate={
                    autoSources.length > 0 &&
                    autoSources.length < AUTO_TRANSLATED_SOURCES.length
                  }
                  onClick={() =>
                    actions.setFilters(toggleAutoTranslated(value))
                  }
                  indicator={<StateIndicator state={state} />}
                />,
                ...AUTO_TRANSLATED_SOURCES.map((source) => (
                  <FilterItem
                    key={source}
                    data-cy="translations-filter-auto-translated-by"
                    data-cy-source={source}
                    label={<AutoTranslatedByLabel source={source} />}
                    selected={autoSources.includes(source)}
                    onClick={() =>
                      actions.setFilters(toggleAutoTranslatedBy(value, source))
                    }
                    indicator={
                      <StyledIcon>{AUTO_TRANSLATED_BY_ICON[source]}</StyledIcon>
                    }
                    indented
                  />
                )),
              ];
            })}
            <CompactListSubheader>
              <Box display="flex" justifyContent="space-between">
                <Box>{t('translations_filter_languages_select_title')}</Box>
              </Box>
            </CompactListSubheader>
            <FilterItem
              data-cy="translations-filter-apply-no-base"
              label={t('translations_filter_languages_no_base')}
              selected={value.filterTranslationLanguage === undefined}
              onClick={() => toggleFilterLanguage(undefined)}
              exclusive
            />
            {expanded && (
              <>
                <FilterItem
                  data-cy="translations-filter-apply-for-all"
                  label={t('translations_filter_languages_all')}
                  selected={value.filterTranslationLanguage === true}
                  onClick={() => toggleFilterLanguage(true)}
                  exclusive
                />
                {selectedLanguages?.map((lang) => {
                  return (
                    <FilterItem
                      data-cy="translations-filter-apply-for-language"
                      key={lang.id}
                      label={lang.name}
                      selected={value.filterTranslationLanguage === lang.tag}
                      onClick={() => toggleFilterLanguage(lang.tag)}
                      exclusive
                    />
                  );
                })}
              </>
            )}
            <MenuItem
              data-cy="translations-filter-apply-for-expand"
              role="button"
              onClick={() => setExpanded((value) => !value)}
              sx={{
                display: 'flex',
                justifyContent: 'center',
              }}
            >
              {expanded ? <ChevronUp /> : <ChevronDown />}
            </MenuItem>
          </Box>
        </Menu>
      )}
    </>
  );
};

export function getTranslationFiltersLength(value: FiltersInternal) {
  return value.filterTranslationState?.length ?? 0;
}

const AUTO_TRANSLATED_BY_ICON: Record<AutoTranslatedBy, ReactNode> = {
  TRANSLATION_MEMORY: <TranslationMemory />,
  MACHINE_TRANSLATION: <Mt />,
};

const AutoTranslatedByLabel = ({ source }: { source: AutoTranslatedBy }) => {
  const { t } = useTranslate();
  switch (source) {
    case 'TRANSLATION_MEMORY':
      return (
        <>
          {t('translation_state_auto_translated_by_tm', 'Translation memory')}
        </>
      );
    case 'MACHINE_TRANSLATION':
      return (
        <>
          {t('translation_state_auto_translated_by_mt', 'Machine translation')}
        </>
      );
    default:
      return exhaustiveMatchingGuard(source);
  }
};

const TranslatedState = (props: {
  state: TranslationStateType;
  autoTranslatedBy: FiltersInternal['filterAutoTranslatedBy'];
}) => {
  const translateState = useStateTranslation();
  if (props.state === 'AUTO_TRANSLATED' && props.autoTranslatedBy) {
    return (
      <T
        keyName="translation_filters_auto_translated_by"
        defaultValue="{state}: {source}"
        params={{
          state: translateState(props.state),
          source: <AutoTranslatedByLabel source={props.autoTranslatedBy} />,
        }}
      />
    );
  }
  return <>{translateState(props.state)}</>;
};

export function getTranslationFiltersName(value: FiltersInternal) {
  if (value.filterTranslationState?.length) {
    return (
      <TranslatedState
        state={value.filterTranslationState[0]}
        autoTranslatedBy={value.filterAutoTranslatedBy}
      />
    );
  }
}
