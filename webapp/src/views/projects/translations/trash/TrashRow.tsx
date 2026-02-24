import React from 'react';
import { Trash01 } from '@untitled-ui/icons-react';
import {
  Button,
  Checkbox,
  Chip,
  IconButton,
  styled,
  Tooltip,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useProject } from 'tg.hooks/useProject';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { confirmation } from 'tg.hooks/confirmation';
import { TRANSLATION_STATES } from 'tg.constants/translationStates';
import { LimitedHeightText } from 'tg.component/LimitedHeightText';
import { Tag } from '../Tags/Tag';
import { components } from 'tg.service/apiSchema.generated';
import ReactMarkdown from 'react-markdown';
import { MarkdownLink } from 'tg.component/common/MarkdownLink';
import { TranslationVisual } from '../translationVisual/TranslationVisual';

type LanguageModel = components['schemas']['LanguageModel'];

const StyledRow = styled('div')`
  display: grid;
  border-top: 1px solid ${({ theme }) => theme.palette.divider};
  position: relative;
`;

const StyledKeyCell = styled('div')`
  display: grid;
  grid-template-columns: auto 1fr;
  grid-template-rows: auto auto auto auto 1fr;
  grid-template-areas:
    'checkbox  key          '
    '.         description  '
    '.         screenshots  '
    '.         tags         '
    '.         .            ';
  position: relative;
  outline: 0;
  overflow: hidden;
  min-width: 0;
`;

const StyledCheckbox = styled(Checkbox)`
  grid-area: checkbox;
  width: 38px;
  height: 38px;
  margin: 3px -9px -9px 3px;
`;

const StyledKey = styled('div')`
  grid-area: key;
  margin: 12px 12px 8px 12px;
  overflow: hidden;
  position: relative;
`;

const StyledDescription = styled('div')`
  grid-area: description;
  padding: 0px 12px 8px 12px;
  font-size: 13px;
  color: ${({ theme }) =>
    theme.palette.mode === 'light'
      ? theme.palette.emphasis[300]
      : theme.palette.emphasis[500]};
`;

const StyledScreenshots = styled('div')`
  grid-area: screenshots;
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
  padding: 0px 12px 8px 12px;
  overflow: hidden;
`;

const StyledScreenshotBox = styled('div')`
  overflow: hidden;
  position: relative;
  border-radius: 4px;
  background: ${({ theme }) => theme.palette.tokens.text._states.selected};

  &::after {
    content: '';
    position: absolute;
    top: 0px;
    left: 0px;
    right: 0px;
    bottom: 0px;
    border-radius: 4px;
    border: 1px solid ${({ theme }) => theme.palette.tokens.border.primary};
    pointer-events: none;
  }
`;

const StyledScreenshotImg = styled('img')`
  width: 100%;
  height: 100%;
  object-fit: contain;
`;

const StyledTags = styled('div')`
  grid-area: tags;
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  min-width: 0;
  & > * {
    margin: 0 6px 3px 0;
  }
  margin: 0px 12px 12px 12px;
  position: relative;
`;

const StyledTrashedCell = styled('div')`
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 8px 12px;
  gap: 6px;
  border-left: 1px solid ${({ theme }) => theme.palette.divider};
`;

const StyledTrashedTime = styled('div')`
  font-size: 13px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledDeletesIn = styled('div')`
  display: flex;
  align-items: center;
  gap: 4px;
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: 12px;
`;

const StyledActions = styled('div')`
  display: flex;
  align-items: center;
  gap: 4px;
`;

const StyledTranslationCell = styled('div')`
  display: grid;
  grid-template-columns: auto 1fr;
  position: relative;
  outline: 0;
  overflow: hidden;
`;

const StyledStateBar = styled('div')`
  height: 100%;
  width: 4px;
  filter: brightness(
    ${({ theme }) => (theme.palette.mode === 'dark' ? 0.7 : 1)}
  );
`;

const StyledTranslationContent = styled('div')`
  display: grid;
  grid-auto-rows: max-content;
  min-height: 23px;
  margin: 8px 12px 8px 12px;
  position: relative;
  align-content: start;
`;

type Props = {
  data: any;
  selected: boolean;
  onToggle: () => void;
  onRestore: () => void;
  onDelete: () => void;
  canRestore: boolean;
  canDelete: boolean;
  languages: LanguageModel[];
  columnSizes: string[];
};

export const TrashRow: React.FC<Props> = React.memo(function TrashRow({
  data,
  selected,
  onToggle,
  onRestore,
  onDelete,
  canRestore,
  canDelete,
  languages,
  columnSizes,
}) {
  const { t } = useTranslate();
  const project = useProject();

  const restoreMutation = useApiMutation({
    url: '/v2/projects/{projectId}/keys/trash/{keyId}/restore' as any,
    method: 'put' as any,
    invalidatePrefix: '/v2/projects/{projectId}/keys/trash' as any,
  });

  const deleteMutation = useApiMutation({
    url: '/v2/projects/{projectId}/keys/trash/{keyId}' as any,
    method: 'delete' as any,
    invalidatePrefix: '/v2/projects/{projectId}/keys/trash' as any,
  });

  const handleRestore = () => {
    restoreMutation.mutate(
      {
        path: { projectId: project.id, keyId: data.id },
      } as any,
      {
        onSuccess: onRestore,
      }
    );
  };

  const handlePermanentDelete = () => {
    confirmation({
      title: t('trash_permanent_delete_title'),
      message: t('trash_permanent_delete_confirmation'),
      onConfirm() {
        deleteMutation.mutate(
          {
            path: { projectId: project.id, keyId: data.id },
          } as any,
          {
            onSuccess: onDelete,
          }
        );
      },
    });
  };

  const deletedAt = new Date(data.deletedAt);
  const permanentDeleteAt = new Date(data.permanentDeleteAt);
  const now = new Date();
  const daysAgo = Math.floor(
    (now.getTime() - deletedAt.getTime()) / (1000 * 60 * 60 * 24)
  );
  const daysUntilDelete = Math.max(
    0,
    Math.ceil(
      (permanentDeleteAt.getTime() - now.getTime()) / (1000 * 60 * 60 * 24)
    )
  );

  const deletedTimeText =
    daysAgo === 0
      ? t('trash_deleted_today')
      : t('trash_deleted_ago', { days: daysAgo });

  const translations = data.translations ?? {};
  const tags = data.tags ?? [];

  return (
    <StyledRow
      style={{
        gridTemplateColumns: columnSizes.join(' '),
        width: `calc(${columnSizes.join(' + ')})`,
      }}
      data-cy="trash-row"
    >
      <StyledKeyCell>
        <StyledCheckbox
          size="small"
          checked={selected}
          onChange={onToggle}
          data-cy="trash-row-checkbox"
        />
        <StyledKey data-cy="trash-key-name">
          <LimitedHeightText maxLines={3} wrap="break-all">
            {data.name}
          </LimitedHeightText>
          {data.namespace && (
            <Chip
              label={data.namespace}
              size="small"
              variant="outlined"
              sx={{ ml: 1, verticalAlign: 'middle' }}
            />
          )}
        </StyledKey>
        {data.description && (
          <StyledDescription data-cy="trash-key-description">
            <LimitedHeightText maxLines={5}>
              <ReactMarkdown
                components={{
                  a: MarkdownLink,
                }}
              >
                {data.description}
              </ReactMarkdown>
            </LimitedHeightText>
          </StyledDescription>
        )}
        {data.screenshots?.length > 0 && (
          <StyledScreenshots>
            {data.screenshots.map((sc: any) => {
              const w = 100;
              const h =
                sc.width && sc.height
                  ? Math.min(w / (sc.width / sc.height), 100)
                  : 100;
              return (
                <StyledScreenshotBox
                  key={sc.id}
                  style={{ width: w, height: h }}
                >
                  <StyledScreenshotImg src={sc.thumbnailUrl} alt="" />
                </StyledScreenshotBox>
              );
            })}
          </StyledScreenshots>
        )}
        {tags.length > 0 && (
          <StyledTags>
            {tags.map((tag: any) => (
              <Tag key={tag.id} name={tag.name} />
            ))}
          </StyledTags>
        )}
      </StyledKeyCell>

      <StyledTrashedCell>
        <StyledTrashedTime>{deletedTimeText}</StyledTrashedTime>
        <StyledDeletesIn>
          <Trash01 width={14} height={14} />
          <T keyName="trash_deletes_in" params={{ days: daysUntilDelete }} />
        </StyledDeletesIn>
        <StyledActions>
          {canRestore && (
            <Button
              size="small"
              color="error"
              variant="outlined"
              onClick={handleRestore}
              disabled={restoreMutation.isLoading}
              data-cy="trash-restore-button"
            >
              <T keyName="trash_restore_button" />
            </Button>
          )}
          {canDelete && (
            <Tooltip
              title={t('trash_permanent_delete_tooltip')}
            >
              <IconButton
                size="small"
                color="error"
                onClick={handlePermanentDelete}
                disabled={deleteMutation.isLoading}
                data-cy="trash-permanent-delete-button"
              >
                <Trash01 width={16} height={16} />
              </IconButton>
            </Tooltip>
          )}
        </StyledActions>
      </StyledTrashedCell>

      {languages.map((language) => {
        const translation = translations[language.tag];
        const state = translation?.state || 'UNTRANSLATED';
        const stateColor =
          TRANSLATION_STATES[state]?.color ||
          TRANSLATION_STATES['UNTRANSLATED'].color;

        return (
          <StyledTranslationCell key={language.tag}>
            <StyledStateBar style={{ borderLeft: `4px solid ${stateColor}` }} />
            <StyledTranslationContent>
              {translation?.text ? (
                <TranslationVisual
                  text={translation.text}
                  locale={language.tag}
                  isPlural={data.isPlural ?? false}
                />
              ) : null}
            </StyledTranslationContent>
          </StyledTranslationCell>
        );
      })}
    </StyledRow>
  );
});
