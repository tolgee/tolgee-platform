import { Box, Button, styled } from '@mui/material';
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
import { Tag } from '../Tags/Tag';
import { TagInput } from '../Tags/TagInput';
import { CellTranslation } from '../TranslationsList/CellTranslation';
import { FieldLabel } from 'tg.component/FormField';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';
import { NamespaceSelector } from 'tg.component/NamespaceSelector/NamespaceSelector';
import { useUrlSearch } from 'tg.hooks/useUrlSearch';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { FloatingToolsPanel } from '../ToolsPanel/FloatingToolsPanel';

const StyledContainer = styled('div')`
  display: grid;
  align-content: start;
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
  border-color: ${({ theme }) => theme.palette.divider1};
  border-width: 1px;
  border-style: solid;
`;

const StyledLanguageField = styled('div')`
  border-color: ${({ theme }) => theme.palette.divider1};
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
  const { satisfiesPermission } = useProjectPermissions();
  const editEnabled = satisfiesPermission('keys.edit');

  const keyData = useTranslationsSelector((c) => c.translations)?.[0];
  const translationOpen = useTranslationsSelector((c) =>
    Boolean(c.cursor?.keyId === keyData?.keyId && c.cursor?.language)
  );

  const languages = useTranslationsSelector((c) => c.languages);
  const selectedLanguages = useTranslationsSelector((c) => c.selectedLanguages);
  const history = useHistory();
  const sidePanelWidth = useTranslationsSelector(
    (c) => c.layout.sidePanelWidth
  );

  const urlId = useUrlSearch().id as string | undefined;
  const [_urlKey, setUrlKey] = useUrlSearchState('key');
  const [_urlNamespace, setUrlNamespace] = useUrlSearchState('ns');

  const handleAddTag = (name: string) => {
    addTag({ keyId: keyData!.keyId, name });
  };

  const { refetchUsage } = useGlobalActions();

  const handleRemoveTag = (tagId: number) => {
    removeTag({ keyId: keyData!.keyId, tagId });
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
      keyId: keyData!.keyId,
      value: { keyNamespace: namespace },
    });
  };

  const handleNamespaceChange = (namespace: string | undefined = '') => {
    const previousNs = keyData!.keyNamespace;
    cacheUpdateNs(namespace);
    updateNamespace.mutate(
      {
        path: { projectId: project.id, id: keyData!.keyId },
        content: {
          'application/json': { name: keyData!.keyName, namespace },
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
            path: { projectId: project.id, ids: [keyData!.keyId] },
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

  return keyData ? (
    <Box display="grid" gridTemplateColumns="1fr auto">
      <StyledContainer>
        <div>
          <FieldLabel>
            <T keyName="translation_single_label_key" />
          </FieldLabel>
          <StyledField data-cy="translation-edit-key-field">
            <CellKey
              data={keyData!}
              editEnabled={editEnabled}
              active={true}
              simple={true}
              onSaveSuccess={(key) => urlId === undefined && setUrlKey(key)}
            />
          </StyledField>
        </div>

        {project.useNamespaces && (
          <div>
            <FieldLabel>
              <T keyName="translation_single_label_namespace" />
            </FieldLabel>
            <NamespaceSelector
              value={keyData.keyNamespace}
              onChange={handleNamespaceChange}
            />
          </div>
        )}

        <div>
          <FieldLabel>
            <T keyName="translation_single_label_tags" />
          </FieldLabel>
          <StyledTags>
            {keyData.keyTags.map((tag) => {
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
          <Box>
            {selectedLanguages?.map((lang) => {
              const language = languages?.find((l) => l.tag === lang);
              return language ? (
                <StyledLanguageField
                  key={lang}
                  data-cy="translation-edit-translation-field"
                >
                  <CellTranslation
                    data={keyData!}
                    language={language}
                    active={true}
                    lastFocusable={false}
                  />
                </StyledLanguageField>
              ) : null;
            })}
          </Box>
        </div>

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
      {translationOpen && Boolean(sidePanelWidth) && (
        <Box ml="-1px" mt="25px">
          <FloatingToolsPanel width={sidePanelWidth} />
        </Box>
      )}
    </Box>
  ) : null;
};
