import {
  Box,
  Card,
  IconButton,
  styled,
  TextField,
  Tooltip,
  Typography,
  useTheme,
} from '@mui/material';
import React, { useState } from 'react';
import { GlossaryTermPreviewProps } from '../../../eeSetup/EeModuleType';
import {
  ArrowNarrowRight,
  BookClosed,
  LinkExternal02,
  Edit02,
  Check,
  XClose,
  Plus,
} from '@untitled-ui/icons-react';
import { GlossaryTermTags } from 'tg.ee.module/glossary/components/GlossaryTermTags';
import { languageInfo } from '@tginternal/language-util/lib/generated/languageInfo';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';
import { T } from '@tolgee/react';
import { Link } from 'react-router-dom';
import { getGlossaryTermSearchUrl } from 'tg.constants/links';
import clsx from 'clsx';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { TooltipCard } from 'tg.component/common/TooltipCard';

const StyledContainer = styled(Box)`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacing(1.5)};

  border-radius: 4px;
  transition: background-color 0.1s ease-in-out, color 0.1s ease-in-out;

  &.slim {
    padding: ${({ theme }) => theme.spacing(0.75)};
    gap: ${({ theme }) => theme.spacing(0.5)};

    &:hover {
      background-color: ${({ theme }) => theme.palette.emphasis[50]};
    }
  }

  &.slim.clickable {
    cursor: pointer;

    &:hover {
      color: ${({ theme }) => theme.palette.primary.main};
    }
  }
`;

const StyledInnerCard = styled(Card)`
  padding: ${({ theme }) => theme.spacing(1.5)};
  background-color: ${({ theme }) => theme.palette.tokens.text._states.hover};
`;

const StyledTitleWrapper = styled(Box)`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1)};
`;

const StyledTitleTextWrapper = styled(Box)`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1)};
  margin-right: ${({ theme }) => theme.spacing(2)};
`;

const StyledTitle = styled(Typography)``;

const StyledEmptyTitle = styled(Typography)`
  color: ${({ theme }) => theme.palette.text.disabled};
  font-style: italic;
`;

const StyledGap = styled('div')`
  flex-grow: 1;
`;

const StyledDescription = styled(Typography)`
  color: ${({ theme }) => theme.palette.text.secondary};
  font-size: 13px;
`;

const StyledEmptyDescription = styled(StyledDescription)`
  font-style: italic;
`;

const StyledEditWrapper = styled(Box)`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1)};
  flex-grow: 1;
`;

const StyledEditInput = styled(TextField)`
  flex-grow: 1;
  & .MuiInputBase-input {
    padding: ${({ theme }) => theme.spacing(0.75, 1)};
  }
`;

export const GlossaryTermPreview: React.VFC<GlossaryTermPreviewProps> = ({
  term,
  languageTag,
  targetLanguageTag,
  appendValue,
  standalone,
  slim,
  editEnabled,
}) => {
  const theme = useTheme();
  const { preferredOrganization } = usePreferredOrganization();
  const [isHovering, setIsHovering] = useState(false);
  const [isEditing, setIsEditing] = useState(false);
  const [editValue, setEditValue] = useState('');

  const realLanguageTag = term.flagNonTranslatable
    ? term.glossary.baseLanguageTag
    : languageTag;
  const translation = term.translations.find(
    (t) => t.languageTag === realLanguageTag
  );
  const targetTranslation = term.translations.find(
    (t) => t.languageTag === targetLanguageTag
  );
  const targetLanguageFlag = targetLanguageTag
    ? languageInfo[targetLanguageTag]?.flags?.[0]
    : undefined;
  const text = targetTranslation?.text || translation?.text || undefined;
  const clickable = appendValue && text && text.length > 0 && !isEditing;

  const editLanguageTag = term.flagNonTranslatable
    ? term.glossary.baseLanguageTag
    : targetLanguageTag || languageTag;
  const editTranslation = term.translations.find(
    (t) => t.languageTag === editLanguageTag
  );

  const saveMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}/terms/{termId}/translations',
    method: 'post',
    invalidatePrefix: [
      '/v2/organizations/{organizationId}/glossaries/{glossaryId}/terms',
      '/v2/projects/{projectId}/glossary-highlights',
    ],
  });

  const handleStartEdit = (e: React.MouseEvent) => {
    e.stopPropagation();
    setEditValue(editTranslation?.text || '');
    setIsEditing(true);
  };

  const handleCancel = (e: React.MouseEvent) => {
    e.stopPropagation();
    setIsEditing(false);
  };

  const handleSave = (e?: React.MouseEvent) => {
    e?.stopPropagation();
    if (saveMutation.isLoading) return;

    saveMutation.mutate(
      {
        path: {
          organizationId: preferredOrganization!.id,
          glossaryId: term.glossary.id,
          termId: term.id,
        },
        content: {
          'application/json': {
            languageTag: editLanguageTag,
            text: editValue,
          },
        },
      },
      {
        onSuccess: () => {
          setIsEditing(false);
        },
      }
    );
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSave();
    } else if (e.key === 'Escape') {
      e.preventDefault();
      setIsEditing(false);
    }
  };

  const content = (
    <StyledContainer
      data-cy="glossary-term-preview-container"
      className={clsx({ slim, clickable })}
      onMouseEnter={() => setIsHovering(true)}
      onMouseLeave={() => setIsHovering(false)}
      onMouseDown={(e) => {
        if (clickable) {
          e.preventDefault();
        }
      }}
      onClick={() => {
        if (clickable) {
          appendValue(text);
        }
      }}
    >
      <StyledTitleWrapper>
        {standalone && !isEditing && <BookClosed />}
        {isEditing ? (
          <StyledEditWrapper>
            <StyledEditInput
              value={editValue}
              onChange={(e) => setEditValue(e.target.value)}
              onKeyDown={handleKeyDown}
              onClick={(e) => e.stopPropagation()}
              autoFocus
              variant="outlined"
              size="small"
              data-cy="glossary-term-preview-edit-input"
            />
            <Tooltip title={<T keyName="translate_glossary_term_cell_save" />}>
              <IconButton
                onClick={handleSave}
                size="small"
                color="primary"
                data-cy="glossary-term-preview-save-button"
              >
                <Check width={20} height={20} />
              </IconButton>
            </Tooltip>
            <Tooltip
              title={<T keyName="translate_glossary_term_cell_cancel" />}
            >
              <IconButton
                onClick={handleCancel}
                size="small"
                data-cy="glossary-term-preview-cancel-button"
              >
                <XClose width={20} height={20} />
              </IconButton>
            </Tooltip>
          </StyledEditWrapper>
        ) : (
          <>
            <StyledTitleTextWrapper>
              <StyledTitle
                variant="body2"
                data-cy="glossary-term-preview-source-text"
              >
                {translation?.text}
              </StyledTitle>
              <ArrowNarrowRight />
              {targetLanguageFlag && (
                <FlagImage width={20} flagEmoji={targetLanguageFlag} />
              )}
              {targetTranslation &&
              languageTag != targetLanguageTag &&
              !term.flagNonTranslatable ? (
                <StyledTitle
                  variant="body2"
                  data-cy="glossary-term-preview-target-text"
                >
                  {targetTranslation.text}
                </StyledTitle>
              ) : (
                <StyledEmptyTitle
                  variant="body2"
                  data-cy="glossary-term-preview-target-text"
                >
                  <T keyName="glossary_term_preview_target_translation_empty" />
                </StyledEmptyTitle>
              )}
            </StyledTitleTextWrapper>
            <StyledGap />
            {(isHovering || standalone) && (
              <>
                {editEnabled && (
                  <Tooltip
                    title={
                      editTranslation?.text ? (
                        <T keyName="glossary_term_preview_edit_translation_tooltip" />
                      ) : (
                        <T keyName="glossary_term_preview_add_translation_tooltip" />
                      )
                    }
                  >
                    <IconButton
                      onClick={handleStartEdit}
                      sx={{
                        margin: theme.spacing(-0.8),
                      }}
                      size="small"
                      data-cy="glossary-term-preview-edit-button"
                    >
                      {editTranslation?.text ? (
                        <Edit02 width={20} height={20} />
                      ) : (
                        <Plus width={20} height={20} />
                      )}
                    </IconButton>
                  </Tooltip>
                )}
                {!slim && (
                  <Tooltip
                    title={
                      <T keyName="glossary_term_preview_open_full_view_tooltip" />
                    }
                  >
                    <IconButton
                      sx={{
                        margin: theme.spacing(-0.8),
                      }}
                      component={Link}
                      to={getGlossaryTermSearchUrl(
                        term.glossary.organizationOwner.slug,
                        term.glossary.id,
                        translation?.text || ''
                      )}
                      target="_blank"
                      rel="noreferrer noopener"
                      size="small"
                    >
                      <LinkExternal02 width={20} height={20} />
                    </IconButton>
                  </Tooltip>
                )}
              </>
            )}
          </>
        )}
      </StyledTitleWrapper>
      <GlossaryTermTags term={term} />
      {!slim && (
        <StyledInnerCard
          elevation={0}
          data-cy="glossary-term-preview-description-card"
        >
          {term.description ? (
            <StyledDescription data-cy="glossary-term-preview-description">
              {term.description}
            </StyledDescription>
          ) : (
            <StyledEmptyDescription data-cy="glossary-term-preview-empty-description">
              <T keyName="empty_description" />
            </StyledEmptyDescription>
          )}
        </StyledInnerCard>
      )}
    </StyledContainer>
  );

  if (slim) {
    return (
      <Tooltip
        placement="bottom-start"
        enterDelay={200}
        components={{ Tooltip: TooltipCard }}
        title={
          <GlossaryTermPreview
            term={term}
            languageTag={languageTag}
            targetLanguageTag={targetLanguageTag}
            standalone
          />
        }
      >
        {content}
      </Tooltip>
    );
  }

  return content;
};
