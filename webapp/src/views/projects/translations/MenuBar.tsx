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
import AddIcon from '@material-ui/icons/Add';
import DeleteIcon from '@material-ui/icons/Delete';
import { T, useTranslate } from '@tolgee/react';
import { FunctionComponent, useContext } from 'react';
import { Link } from 'react-router-dom';
import { LanguagesMenu } from 'tg.component/common/form/LanguagesMenu';
import { LINKS, PARAMS } from 'tg.constants/links';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { confirmation } from 'tg.hooks/confirmation';
import { useProject } from 'tg.hooks/useProject';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { MessageService } from 'tg.service/MessageService';
import { ProjectPermissionType } from 'tg.service/response.types';
import { TranslationActions } from 'tg.store/project/TranslationActions';
import { container } from 'tsyringe';
import { TranslationsSearchField } from './TranslationsSearchField';
import { TranslationListContext } from './TtranslationsGridContextProvider';

const messaging = container.resolve(MessageService);

export const MenuBar: FunctionComponent = () => {
  const project = useProject();

  const actions = container.resolve(TranslationActions);
  const listContext = useContext(TranslationListContext);
  const projectPermissions = useProjectPermissions();

  const t = useTranslate();

  const deleteKey = useApiMutation({
    url: '/api/project/{projectId}/keys',
    method: 'delete',
    invalidatePrefix: '/api/project',
  });

  const handleConfirm = () => {
    deleteKey.mutate(
      {
        path: { projectId: project.id },
        content: { 'application/json': Array.from(listContext.checkedKeys) },
      },
      {
        onError: (err) => {
          for (const error of parseErrorResponse(err)) {
            messaging.error(<T>{error}</T>);
          }
        },
        onSuccess: () => {
          messaging.success(<T>Translation grid - Successfully deleted!</T>);
          actions.setTranslationEditing.dispatch({
            data: null,
            skipConfirm: true,
          });
        },
      }
    );
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
