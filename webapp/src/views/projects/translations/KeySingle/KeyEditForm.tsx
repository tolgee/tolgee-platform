import { Button, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useHistory } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { confirmation } from 'tg.hooks/confirmation';
import { useProject } from 'tg.hooks/useProject';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';

import { CellKey } from '../CellKey';
import {
  useTranslationsActions,
  useTranslationsSelector,
} from '../context/TranslationsContext';
import { ScreenshotGallery } from '../Screenshots/ScreenshotGallery';
import { Tag } from '../Tags/Tag';
import { TagInput } from '../Tags/TagInput';
import { CellTranslation } from '../TranslationsList/CellTranslation';
import { FieldLabel } from 'tg.component/FormField';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { NamespaceSelector } from 'tg.component/NamespaceSelector/NamespaceSelector';
import { useUrlSearch } from 'tg.hooks/useUrlSearch';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';

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
  const { addTag, removeTag, updateKey } = useTranslationsActions();
  const { t } = useTranslate();
  const project = useProject();
  const { satisfiesLanguageAccess, satisfiesPermission } =
    useProjectPermissions();
  const canViewScreenshots = satisfiesPermission('screenshots.view');
  const editEnabled = satisfiesPermission('keys.edit');

  const translation = useTranslationsSelector((c) => c.translations)?.[0];
  const languages = useTranslationsSelector((c) => c.languages);
  const selectedLanguages = useTranslationsSelector((c) => c.selectedLanguages);
  const history = useHistory();

  const urlId = useUrlSearch().id as string | undefined;
  const [_urlKey, setUrlKey] = useUrlSearchState('key');
  const [_urlNamespace, setUrlNamespace] = useUrlSearchState('ns');

  const handleAddTag = (name: string) => {
    addTag({ keyId: translation!.keyId, name });
  };

  const { refetchUsage } = useGlobalActions();

  const handleRemoveTag = (tagId: number) => {
    removeTag({ keyId: translation!.keyId, tagId });
  };

  const deleteKeys = useApiMutation({
    url: '/v2/projects/{projectId}/keys/{ids}',
    method: 'delete',
  });

  const updateNamespace = useApiMutation({
    url: '/v2/projects/{projectId}/keys/{id}',
    method: 'put',
  });

  const cacheUpdateNs = (namespace: string) => {
    updateKey({
      keyId: translation!.keyId,
      value: { keyNamespace: namespace },
    });
  };

  const handleNamespaceChange = (namespace: string | undefined = '') => {
    const previousNs = translation!.keyNamespace;
    cacheUpdateNs(namespace);
    updateNamespace.mutate(
      {
        path: { projectId: project.id, id: translation!.keyId },
        content: {
          'application/json': { name: translation!.keyName, namespace },
        },
      },
      {
        onError(e) {
          cacheUpdateNs(previousNs || '');
        },
        onSuccess() {
          if (urlId === undefined) {
            setUrlNamespace(namespace || undefined);
          }
        },
      }
    );
  };

  const handleRemoveKey = () => {
    confirmation({
      title: <T keyName="translation_single_delete_title" />,
      message: <T keyName="translation_single_delete_text" />,
      onConfirm() {
        deleteKeys.mutate(
          {
            path: { projectId: project.id, ids: [translation!.keyId] },
          },
          {
            onSuccess() {
              messageService.success(
                <T keyName="translation_single_delete_success" />
              );
              history.push(
                LINKS.PROJECT_TRANSLATIONS.build({
                  [PARAMS.PROJECT_ID]: project.id,
                })
              );
              refetchUsage();
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
          <T keyName="translation_single_label_key" />
        </FieldLabel>
        <StyledField data-cy="translation-edit-key-field">
          <CellKey
            data={translation!}
            editEnabled={editEnabled}
            active={true}
            simple={true}
            onSaveSuccess={(key) => urlId === undefined && setUrlKey(key)}
          />
        </StyledField>
      </div>

      <div>
        <FieldLabel>
          <T keyName="translation_single_label_namespace" />
        </FieldLabel>
        <NamespaceSelector
          value={translation.keyNamespace}
          onChange={handleNamespaceChange}
        />
      </div>

      <div>
        <FieldLabel>
          <T keyName="translation_single_label_tags" />
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
          <T keyName="translation_single_translations_title" />
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
                editEnabled={satisfiesLanguageAccess(
                  'translations.edit',
                  language.id
                )}
                stateChangeEnabled={satisfiesLanguageAccess(
                  'translations.state-edit',
                  language.id
                )}
                lastFocusable={false}
              />
            </StyledLanguageField>
          ) : null;
        })}
      </div>

      {canViewScreenshots && (
        <div>
          <FieldLabel>
            <T keyName="translation_single_label_screenshots" />
          </FieldLabel>
          <StyledGalleryField>
            <ScreenshotGallery keyId={translation!.keyId} />
          </StyledGalleryField>
        </div>
      )}

      <StyledActions>
        {editEnabled && (
          <Button
            color="secondary"
            variant="outlined"
            onClick={handleRemoveKey}
            data-cy="translation-edit-delete-button"
          >
            <T keyName="translation_single_label_delete" />
          </Button>
        )}
      </StyledActions>
    </StyledContainer>
  ) : null;
};
