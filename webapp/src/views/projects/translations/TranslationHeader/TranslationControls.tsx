import { ViewListRounded, AppsRounded, Add, Delete } from '@mui/icons-material';
import { Button, ButtonGroup, IconButton, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { LanguagesSelect } from 'tg.component/common/form/LanguagesSelect/LanguagesSelect';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { ProjectPermissionType } from 'tg.service/response.types';
import { useTopBarHidden } from 'tg.component/layout/TopBar/TopBarContext';
import TranslationsSearchField from './TranslationsSearchField';

import {
  useTranslationsSelector,
  useTranslationsDispatch,
} from '../context/TranslationsContext';
import { Filters } from '../Filters/Filters';
import { ViewMode } from '../context/types';

const StyledControls = styled('div')`
  display: flex;
  box-sizing: border-box;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  margin: -12px -5px -10px -5px;
  margin-left: ${({ theme }) => theme.spacing(-2)};
  margin-right: ${({ theme }) => theme.spacing(-2)};
  padding: ${({ theme }) => theme.spacing(0, 1.5)};
  position: sticky;
  top: 50px;
  height: 61px;
  z-index: ${({ theme }) => theme.zIndex.appBar + 1};
  background: ${({ theme }) => theme.palette.background.default};
  transition: transform 0.2s ease-in-out;
  padding-bottom: 8px;
  padding-top: 13px;
`;

const StyledShadow = styled('div')`
  background: ${({ theme }) => theme.palette.background.default};
  height: 1px;
  position: sticky;
  z-index: ${({ theme }) => theme.zIndex.appBar};
  margin-left: ${({ theme }) => theme.spacing(-1)};
  margin-right: ${({ theme }) => theme.spacing(-1)};
  box-shadow: ${({ theme }) =>
    theme.palette.mode === 'dark'
      ? '0px 1px 6px 0px #000000, 0px 1px 6px 0px #000000'
      : '0px -1px 7px 0px #000000'};
  top: 110px;
  transition: all 0.25s;
`;

const StyledSpaced = styled('div')`
  display: flex;
  gap: 10px;
  padding: 0px 5px;
  flex-wrap: wrap;
`;

const StyledDeleteButton = styled(IconButton)`
  display: flex;
  flex-shrink: 1;
  width: 38px;
  height: 38px;
  margin-left: 3px;
`;

const StyledTranslationsSearchField = styled(TranslationsSearchField)`
  min-width: 200px;
`;

const StyledToggleButton = styled(Button)`
  padding: 4px 8px;
`;

type Props = {
  onDialogOpen: () => void;
};

export const TranslationControls: React.FC<Props> = ({ onDialogOpen }) => {
  const projectPermissions = useProjectPermissions();
  const search = useTranslationsSelector((v) => v.search);
  const languages = useTranslationsSelector((v) => v.languages);
  const t = useTranslate();

  const dispatch = useTranslationsDispatch();
  const selection = useTranslationsSelector((v) => v.selection);
  const view = useTranslationsSelector((v) => v.view);
  const selectedLanguages = useTranslationsSelector((c) => c.selectedLanguages);

  const handleSearchChange = (value: string) => {
    dispatch({ type: 'SET_SEARCH', payload: value });
  };

  const handleLanguageChange = (languages: string[]) => {
    dispatch({
      type: 'SELECT_LANGUAGES',
      payload: languages,
    });
  };

  const handleViewChange = (val: ViewMode) => {
    dispatch({ type: 'CHANGE_VIEW', payload: val });
  };

  const handleDelete = () => {
    dispatch({ type: 'DELETE_TRANSLATIONS' });
  };

  const handleAddTranslation = () => {
    onDialogOpen();
  };

  const trigger = useTopBarHidden();

  return (
    <>
      <StyledControls
        style={{
          transform: trigger ? 'translate(0px, -55px)' : 'translate(0px, 0px)',
        }}
      >
        <StyledSpaced>
          {selection.length > 0 && (
            <StyledDeleteButton
              onClick={handleDelete}
              data-cy="translations-delete-button"
              size="large"
            >
              <Delete />
            </StyledDeleteButton>
          )}
          <StyledTranslationsSearchField
            value={search || ''}
            onSearchChange={handleSearchChange}
            label={null}
            variant="outlined"
            placeholder={t('standard_search_label')}
          />
          <Filters />
        </StyledSpaced>

        <StyledSpaced>
          <LanguagesSelect
            onChange={handleLanguageChange}
            value={selectedLanguages || []}
            languages={languages || []}
            context="translations"
          />
          <ButtonGroup>
            <StyledToggleButton
              color={view === 'LIST' ? 'primary' : 'default'}
              onClick={() => handleViewChange('LIST')}
              data-cy="translations-view-list-button"
            >
              <ViewListRounded />
            </StyledToggleButton>
            <StyledToggleButton
              color={view === 'TABLE' ? 'primary' : 'default'}
              onClick={() => handleViewChange('TABLE')}
              data-cy="translations-view-table-button"
            >
              <AppsRounded />
            </StyledToggleButton>
          </ButtonGroup>

          {projectPermissions.satisfiesPermission(
            ProjectPermissionType.EDIT
          ) && (
            <Button
              startIcon={<Add />}
              color="primary"
              variant="contained"
              onClick={handleAddTranslation}
              data-cy="translations-add-button"
            >
              <T>language_create_add</T>
            </Button>
          )}
        </StyledSpaced>
      </StyledControls>
      <StyledShadow
        style={{
          transform: trigger ? 'translate(0px, -55px)' : 'translate(0px, 0px)',
        }}
      />
    </>
  );
};
