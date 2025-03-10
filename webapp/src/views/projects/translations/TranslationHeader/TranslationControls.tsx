import { LayoutGrid02, LayoutLeft, Plus } from '@untitled-ui/icons-react';
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
import { TranslationFilters } from 'tg.component/translation/translationFilters/TranslationFilters';
import { QuickStartHighlight } from 'tg.component/layout/QuickStartGuide/QuickStartHighlight';
import { HeaderSearchField } from 'tg.component/layout/HeaderSearchField';

import {
  useTranslationsActions,
  useTranslationsSelector,
} from '../context/TranslationsContext';
import { Sort } from 'tg.component/CustomIcons';
import { TranslationOrderMenu } from 'tg.component/translation/translationOrder/TranslationOrderMenu';
import { useState } from 'react';

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
  const { satisfiesPermission } = useProjectPermissions();
  const canCreateKeys = satisfiesPermission('keys.create');
  const search = useTranslationsSelector((v) => v.search);
  const languages = useTranslationsSelector((v) => v.languages);
  const { t } = useTranslate();
  const [anchorOrderEl, setAnchorOrderEl] = useState<HTMLButtonElement | null>(
    null
  );

  const { setSearch, selectLanguages, changeView, setOrder } =
    useTranslationsActions();
  const view = useTranslationsSelector((v) => v.view);
  const selectedLanguages = useTranslationsSelector((c) => c.selectedLanguages);
  const allLanguages = useTranslationsSelector((c) => c.languages);
  const filters = useTranslationsSelector((c) => c.filters);
  const order = useTranslationsSelector((c) => c.order);
  const { setFilters } = useTranslationsActions();
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
          selectedLanguages={selectedLanguagesMapped}
          value={filters}
          onChange={setFilters}
        />

        <Tooltip title={t('translation_controls_order_tooltip')}>
          <Badge
            color="primary"
            variant="dot"
            badgeContent={order === 'keyName' ? 0 : 1}
            overlap="circular"
          >
            <IconButton onClick={(e) => setAnchorOrderEl(e.currentTarget)}>
              <Sort />
            </IconButton>
          </Badge>
        </Tooltip>

        <TranslationOrderMenu
          anchorEl={anchorOrderEl}
          onClose={() => setAnchorOrderEl(null)}
          onChange={setOrder}
          value={order}
        />
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
