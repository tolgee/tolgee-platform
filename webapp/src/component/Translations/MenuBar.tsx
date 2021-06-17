import { useQueryClient } from 'react-query';
import { FunctionComponent, useContext } from 'react';
import {
  Box,
  Button,
  FormControlLabel,
  FormGroup,
  IconButton,
  Slide,
  Switch,
  Tooltip,
} from '@material-ui/core';
import { confirmation } from '../../hooks/confirmation';
import DeleteIcon from '@material-ui/icons/Delete';
import { LanguagesMenu } from '../common/form/LanguagesMenu';
import { TranslationsSearchField } from './TranslationsSearchField';
import { Link } from 'react-router-dom';
import { LINKS, PARAMS } from '../../constants/links';
import AddIcon from '@material-ui/icons/Add';
import { TranslationListContext } from './TtranslationsGridContextProvider';
import { useProject } from '../../hooks/useProject';
import { container } from 'tsyringe';
import { TranslationActions } from '../../store/project/TranslationActions';
import { T, useTranslate } from '@tolgee/react';
import { useProjectPermissions } from '../../hooks/useProjectPermissions';
import { ProjectPermissionType } from '../../service/response.types';
import { MessageService } from '../../service/MessageService';
import { useDeleteKey } from '../../service/hooks/Translation';
import { parseErrorResponse } from '../../fixtures/errorFIxtures';

const messaging = container.resolve(MessageService);

export const MenuBar: FunctionComponent = () => {
  const project = useProject();
  const queryClient = useQueryClient();

  const actions = container.resolve(TranslationActions);
  const listContext = useContext(TranslationListContext);
  const projectPermissions = useProjectPermissions();

  const t = useTranslate();

  const deleteKey = useDeleteKey(project.id);

  const handleConfirm = () => {
    deleteKey.mutate(Array.from(listContext.checkedKeys), {
      onError: (err) => {
        for (const error of parseErrorResponse(err)) {
          messaging.error(<T>{error}</T>);
        }
      },
      onSuccess: () => {
        messaging.success(<T>Translation grid - Successfully deleted!</T>);
        queryClient.invalidateQueries(['project']);
        actions.setTranslationEditing.dispatch({
          data: null,
          skipConfirm: true,
        });
      },
    });
  };

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
                      onConfirm: handleConfirm,
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
              <LanguagesMenu
                context="translations"
                defaultSelected={
                  listContext.listLoadable?.data?.params?.languages
                }
              />
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
        {projectPermissions.satisfiesPermission(ProjectPermissionType.EDIT) && (
          <Box display="flex" alignItems="flex-end">
            <Button
              component={Link}
              variant="outlined"
              color="primary"
              size={'small'}
              to={LINKS.PROJECT_TRANSLATIONS_ADD.build({
                [PARAMS.PROJECT_ID]: project.id,
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
