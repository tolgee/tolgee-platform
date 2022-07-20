import { container } from 'tsyringe';
import { Button, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useHistory } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { confirmation } from 'tg.hooks/confirmation';
import { useProject } from 'tg.hooks/useProject';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { MessageService } from 'tg.service/MessageService';

import { CellKey } from '../CellKey';
import {
  useTranslationsSelector,
  useTranslationsDispatch,
} from '../context/TranslationsContext';
import { ScreenshotGallery } from '../Screenshots/ScreenshotGallery';
import { Tag } from '../Tags/Tag';
import { TagInput } from '../Tags/TagInput';
import { CellTranslation } from '../TranslationsList/CellTranslation';
import { parseErrorResponse } from 'tg.fixtures/errorFIxtures';
import { FieldLabel } from './FieldLabel';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { ProjectPermissionType } from 'tg.service/response.types';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { useOrganizationUsageMethods } from 'tg.globalContext/helpers';

const messaging = container.resolve(MessageService);

const StyledContainer = styled('div')`
  display: grid;
  row-gap: ${({ theme }) => theme.spacing(2)};
  margin-bottom: ${({ theme }) => theme.spacing(2)};
`;

const StyledTags = styled('div')`
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  overflow: hidden;
  & > * {
    margin: 0px 3px 3px 0px;
  }
  position: relative;
`;

const StyledField = styled('div')`
  border-color: ${({ theme }) => theme.palette.emphasis[200]};
  border-width: 1px;
  border-style: solid;
`;

const StyledGalleryField = styled('div')`
  border-color: ${({ theme }) => theme.palette.emphasis[200]};
  border-width: 1px;
  border-style: solid;
  padding: 2px;
`;

const StyledLanguageField = styled('div')`
  border-color: ${({ theme }) => theme.palette.emphasis[200]};
  border-width: 1px 1px 1px 0px;
  border-style: solid;
  & + & {
    border-top: 0px;
  }
`;

const StyledActions = styled('div')`
  margin-top: 20px;
`;

export const KeyEditForm: React.FC = () => {
  const dispatch = useTranslationsDispatch();
  const t = useTranslate();
  const project = useProject();
  const permissions = useProjectPermissions();

  const translation = useTranslationsSelector((c) => c.translations)?.[0];
  const languages = useTranslationsSelector((c) => c.languages);
  const selectedLanguages = useTranslationsSelector((c) => c.selectedLanguages);
  const history = useHistory();

  const [_urlKey, setUrlKey] = useUrlSearchState('key');

  const handleAddTag = (name: string) => {
    dispatch({
      type: 'ADD_TAG',
      payload: { keyId: translation!.keyId, name },
    });
  };

  const { refetchUsage } = useOrganizationUsageMethods();

  const handleRemoveTag = (tagId: number) => {
    dispatch({
      type: 'REMOVE_TAG',
      payload: { keyId: translation!.keyId, tagId },
    });
  };

  const deleteKeys = useApiMutation({
    url: '/v2/projects/{projectId}/keys/{ids}',
    method: 'delete',
  });

  const editEnabled = permissions.satisfiesPermission(
    ProjectPermissionType.EDIT
  );

  const handleRemoveKey = () => {
    confirmation({
      title: <T>translation_single_delete_title</T>,
      message: <T>translation_single_delete_text</T>,
      onConfirm() {
        deleteKeys.mutate(
          {
            path: { projectId: project.id, ids: [translation!.keyId] },
          },
          {
            onSuccess() {
              messaging.success(<T>translation_single_delete_success</T>);
              history.push(
                LINKS.PROJECT_TRANSLATIONS.build({
                  [PARAMS.PROJECT_ID]: project.id,
                })
              );
              refetchUsage();
            },
            onError(e) {
              const parsed = parseErrorResponse(e);
              parsed.forEach((error) => messaging.error(<T>{error}</T>));
            },
          }
        );
      },
    });
  };

  return translation ? (
    <StyledContainer>
      <div>
        <FieldLabel>
          <T>translation_single_label_key</T>
        </FieldLabel>
        <StyledField data-cy="translation-edit-key-field">
          <CellKey
            data={translation!}
            editEnabled={editEnabled}
            active={true}
            simple={true}
            onSaveSuccess={(key) => setUrlKey(key)}
          />
        </StyledField>
      </div>

      <div>
        <FieldLabel>
          <T>translation_single_label_tags</T>
        </FieldLabel>
        <StyledTags>
          {translation.keyTags.map((tag) => {
            return (
              <Tag
                key={tag.id}
                name={tag.name}
                onDelete={
                  editEnabled ? () => handleRemoveTag(tag.id) : undefined
                }
              />
            );
          })}
          {editEnabled && (
            <TagInput
              onAdd={handleAddTag}
              placeholder={t('translation_single_tag_placeholder')}
            />
          )}
        </StyledTags>
      </div>

      <div>
        <FieldLabel>
          <T>translation_single_translations_title</T>
        </FieldLabel>
        {selectedLanguages?.map((lang) => {
          const language = languages?.find((l) => l.tag === lang);
          return language ? (
            <StyledLanguageField
              key={lang}
              data-cy="translation-edit-translation-field"
            >
              <CellTranslation
                data={translation!}
                language={language}
                active={true}
                editEnabled={permissions.canEditLanguage(language.id)}
                lastFocusable={false}
              />
            </StyledLanguageField>
          ) : null;
        })}
      </div>

      <div>
        <FieldLabel>
          <T>translation_single_label_screenshots</T>
        </FieldLabel>
        <StyledGalleryField>
          <ScreenshotGallery keyId={translation!.keyId} />
        </StyledGalleryField>
      </div>

      <StyledActions>
        {editEnabled && (
          <Button
            color="secondary"
            variant="outlined"
            onClick={handleRemoveKey}
            data-cy="translation-edit-delete-button"
          >
            <T>translation_single_label_delete</T>
          </Button>
        )}
      </StyledActions>
    </StyledContainer>
  ) : null;
};
