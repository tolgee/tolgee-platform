import {default as React, FunctionComponent, useContext} from 'react';
import {Box, Button, FormControlLabel, FormGroup, IconButton, Slide, Switch, Tooltip,} from '@material-ui/core';
import {confirmation} from '../../hooks/confirmation';
import DeleteIcon from '@material-ui/icons/Delete';
import {LanguagesMenu} from '../common/form/LanguagesMenu';
import {TranslationsSearchField} from './TranslationsSearchField';
import {Link} from 'react-router-dom';
import {LINKS, PARAMS} from '../../constants/links';
import AddIcon from '@material-ui/icons/Add';
import {TranslationListContext} from './TtranslationsGridContextProvider';
import {useProject} from '../../hooks/useProject';
import {container} from 'tsyringe';
import {TranslationActions} from '../../store/project/TranslationActions';
import {T, useTranslate} from '@tolgee/react';
import {useProjectPermissions} from '../../hooks/useProjectPermissions';
import {ProjectPermissionType} from '../../service/response.types';

export const MenuBar: FunctionComponent = () => {
  let projectDTO = useProject();
  const actions = container.resolve(TranslationActions);
  const listContext = useContext(TranslationListContext);
  const projectPermissions = useProjectPermissions();

  const t = useTranslate();

  return (
    <Box mb={2}>
      <Box display="flex">
        <Box flexGrow={1} display="flex">
          <Slide
            in={listContext.isSomeChecked()}
            direction="right"
            mountOnEnter
            unmountOnExit
          >
            <Box pr={2} ml={-2}>
              <Tooltip title={<T>translations_delete_selected</T>}>
                <IconButton
                  color="secondary"
                  data-cy="translations-delete-button"
                  onClick={() =>
                    confirmation({
                      onConfirm: () =>
                        actions.loadableActions.delete.dispatch(
                          projectDTO.id,
                          Array.from(listContext.checkedKeys)
                        ),
                      confirmButtonText: 'Delete',
                      confirmButtonColor: 'secondary',
                      message: (
                        <T
                          parameters={{
                            count: listContext.checkedKeys.size.toString(),
                          }}
                        >
                          translations_key_delete_confirmation_text
                        </T>
                      ),
                      title: <T>global_delete_confirmation</T>,
                    })
                  }
                >
                  <DeleteIcon />
                </IconButton>
              </Tooltip>
            </Box>
          </Slide>
          <Box flexGrow={1} display="flex" alignItems="flex-end">
            <Box pr={2}>
              <LanguagesMenu context="translations" />
            </Box>
            <Box pr={2}>
              <TranslationsSearchField />
            </Box>
            <FormGroup>
              <FormControlLabel
                labelPlacement="start"
                control={
                  <Switch
                    color={'primary'}
                    size="small"
                    checked={listContext.showKeys}
                    onChange={(e) =>
                      listContext.setShowKeys(!!e.target.checked)
                    }
                  />
                }
                label={t('show_keys')}
              />
            </FormGroup>
          </Box>
        </Box>
        {projectPermissions.satisfiesPermission(
          ProjectPermissionType.EDIT
        ) && (
          <Box display="flex" alignItems="flex-end">
            <Button
              component={Link}
              variant="outlined"
              color="primary"
              size={'small'}
              to={LINKS.REPOSITORY_TRANSLATIONS_ADD.build({
                [PARAMS.REPOSITORY_ID]: projectDTO.id,
              })}
              startIcon={<AddIcon />}
            >
              <T>translation_add</T>
            </Button>
          </Box>
        )}
      </Box>
    </Box>
  );
};
