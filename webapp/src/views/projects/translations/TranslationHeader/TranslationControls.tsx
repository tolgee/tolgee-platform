import {
  LayoutGrid02,
  LayoutLeft,
  Plus,
  Trash01,
} from '@untitled-ui/icons-react';
import {
  Badge,
  Button,
  ButtonGroup,
  IconButton,
  styled,
  Tooltip,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { LanguagesSelect } from 'tg.component/common/form/LanguagesSelect/LanguagesSelect';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { TranslationFilters } from 'tg.views/projects/translations/TranslationFilters/TranslationFilters';
import { QuickStartHighlight } from 'tg.component/layout/QuickStartGuide/QuickStartHighlight';
import { HeaderSearchField } from 'tg.component/layout/HeaderSearchField';

import {
  useTranslationsActions,
  useTranslationsSelector,
} from '../context/TranslationsContext';
import { Sort } from 'tg.component/CustomIcons';
import { TranslationSortMenu } from 'tg.component/translation/translationSort/TranslationSortMenu';
import { useState } from 'react';
import { useHistory } from 'react-router-dom';
import { useProject } from 'tg.hooks/useProject';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useTrashCount } from '../trash/useTrashCount';

const StyledContainer = styled('div')`
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: start;
`;

const StyledSpaced = styled('div')`
  display: flex;
  gap: 10px;
  padding: 0px 5px;
`;

const StyledTranslationsSearchField = styled(HeaderSearchField)`
  width: 200px;
`;

const StyledToggleButton = styled(Button)`
  padding: 4px 8px;
`;

type Props = {
  onDialogOpen: () => void;
};

export const TranslationControls: React.FC<Props> = ({ onDialogOpen }) => {
  const { satisfiesPermissionWithBranching } = useProjectPermissions();
  const project = useProject();
  const history = useHistory();
  const canCreateKeys = satisfiesPermissionWithBranching('keys.create');
  const search = useTranslationsSelector((v) => v.search);
  const languages = useTranslationsSelector((v) => v.languages);
  const { t } = useTranslate();
  const [anchorSortEl, setAnchorSortEl] = useState<HTMLButtonElement | null>(
    null
  );
  const trashCount = useTrashCount(project.id);

  const { setSearch, selectLanguages, changeView, setOrder } =
    useTranslationsActions();
  const view = useTranslationsSelector((v) => v.view);
  const selectedLanguages = useTranslationsSelector((c) => c.selectedLanguages);
  const allLanguages = useTranslationsSelector((c) => c.languages);
  const filters = useTranslationsSelector((c) => c.filters);
  const order = useTranslationsSelector((c) => c.order);
  const { setFilters, removeFilter, addFilter } = useTranslationsActions();
  const selectedLanguagesMapped =
    allLanguages?.filter((l) => selectedLanguages?.includes(l.tag)) ?? [];

  const handleAddTranslation = () => {
    onDialogOpen();
  };

  return (
    <StyledContainer>
      <StyledSpaced>
        <StyledTranslationsSearchField
          value={search || ''}
          onSearchChange={setSearch}
          label={null}
          variant="outlined"
          placeholder={t('standard_search_label')}
        />
        <TranslationFilters
          projectId={project.id}
          selectedLanguages={selectedLanguagesMapped}
          value={filters}
          actions={{ setFilters, removeFilter, addFilter }}
        />

        <Tooltip title={t('translation_controls_sort_tooltip')}>
          <Badge
            color="primary"
            variant="dot"
            badgeContent={order === 'keyName' ? 0 : 1}
            overlap="circular"
          >
            <IconButton
              onClick={(e) => setAnchorSortEl(e.currentTarget)}
              data-cy="translation-controls-sort"
            >
              <Sort />
            </IconButton>
          </Badge>
        </Tooltip>

        <TranslationSortMenu
          anchorEl={anchorSortEl}
          onClose={() => setAnchorSortEl(null)}
          onChange={setOrder}
          value={order}
        />

        {trashCount > 0 && (
          <Tooltip title={t('translation_controls_trash_tooltip')}>
            <IconButton
              onClick={() =>
                history.push(
                  LINKS.PROJECT_TRANSLATIONS_TRASH.build({
                    [PARAMS.PROJECT_ID]: project.id,
                  })
                )
              }
              data-cy="translations-trash-button"
            >
              <Trash01 />
            </IconButton>
          </Tooltip>
        )}
      </StyledSpaced>

      <StyledSpaced>
        <LanguagesSelect
          onChange={selectLanguages}
          value={selectedLanguages || []}
          languages={languages || []}
          context="translations"
        />
        <ButtonGroup>
          <StyledToggleButton
            color={view === 'LIST' ? 'primary' : 'default'}
            onClick={() => changeView('LIST')}
            data-cy="translations-view-list-button"
          >
            <LayoutLeft />
          </StyledToggleButton>
          <StyledToggleButton
            color={view === 'TABLE' ? 'primary' : 'default'}
            onClick={() => changeView('TABLE')}
            data-cy="translations-view-table-button"
          >
            <LayoutGrid02 />
          </StyledToggleButton>
        </ButtonGroup>

        {canCreateKeys && (
          <QuickStartHighlight itemKey="add_key">
            <Button
              startIcon={<Plus width={19} height={19} />}
              color="primary"
              variant="contained"
              onClick={handleAddTranslation}
              data-cy="translations-add-button"
            >
              <T keyName="key_add" />
            </Button>
          </QuickStartHighlight>
        )}
      </StyledSpaced>
    </StyledContainer>
  );
};
