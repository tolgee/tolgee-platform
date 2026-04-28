import React, { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Dialog,
  DialogContent,
  DialogTitle,
  IconButton,
  MenuItem,
  Select,
  styled,
  TextField,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { XClose, Plus } from '@untitled-ui/icons-react';
import { languageInfo } from '@tginternal/language-util/lib/generated/languageInfo';
import { FlagImage } from '@tginternal/library/components/languages/FlagImage';

const StyledActions = styled('div')`
  display: flex;
  gap: 8px;
  padding-top: 16px;
  justify-content: end;
`;

const StyledLangRow = styled('div')`
  display: flex;
  gap: ${({ theme }) => theme.spacing(1)};
  align-items: flex-start;
`;

const StyledLangLabel = styled('div')`
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: ${({ theme }) => theme.palette.text.secondary};
  margin-bottom: 4px;
`;

type TargetEntry = {
  languageTag: string;
  text: string;
};

type Props = {
  open: boolean;
  onClose: () => void;
  onFinished: () => void;
  organizationId: number;
  translationMemoryId: number;
  sourceLanguageTag: string;
  availableLanguages: string[];
};

export const TranslationMemoryCreateEntryDialog: React.VFC<Props> = ({
  open,
  onClose,
  onFinished,
  organizationId,
  translationMemoryId,
  sourceLanguageTag,
  availableLanguages,
}) => {
  const { t } = useTranslate();
  const [sourceText, setSourceText] = useState('');
  const [targets, setTargets] = useState<TargetEntry[]>(() =>
    availableLanguages.length > 0
      ? [{ languageTag: availableLanguages[0], text: '' }]
      : []
  );
  const [saving, setSaving] = useState(false);

  // If the dialog was mounted before the org languages finished loading,
  // `availableLanguages` was empty and `targets` initialized to []. Once
  // languages arrive, seed the first target so the user has something to
  // type into. Empty entries should never be the steady state.
  useEffect(() => {
    if (targets.length === 0 && availableLanguages.length > 0) {
      setTargets([{ languageTag: availableLanguages[0], text: '' }]);
    }
  }, [availableLanguages, targets.length]);

  const createMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/translation-memories/{translationMemoryId}/entries',
    method: 'post',
    invalidatePrefix: '/v2/organizations/{organizationId}/translation-memories',
  });

  const updateTarget = (index: number, text: string) => {
    setTargets((prev) =>
      prev.map((t, i) => (i === index ? { ...t, text } : t))
    );
  };

  const removeTarget = (index: number) => {
    setTargets((prev) => prev.filter((_, i) => i !== index));
  };

  const addTarget = () => {
    const usedTags = targets.map((t) => t.languageTag);
    const nextTag = availableLanguages.find((t) => !usedTags.includes(t));
    if (nextTag) {
      setTargets((prev) => [...prev, { languageTag: nextTag, text: '' }]);
    }
  };

  const changeLanguage = (index: number, tag: string) => {
    setTargets((prev) =>
      prev.map((t, i) => (i === index ? { ...t, languageTag: tag } : t))
    );
  };

  const usedTags = targets.map((t) => t.languageTag);
  const canAddMore = availableLanguages.some((t) => !usedTags.includes(t));
  const canSave =
    sourceText.trim() && targets.some((t) => t.text.trim()) && !saving;

  const handleSave = async () => {
    const nonEmpty = targets.filter((t) => t.text.trim());
    if (!sourceText.trim() || nonEmpty.length === 0) return;

    setSaving(true);
    try {
      for (const target of nonEmpty) {
        await createMutation.mutateAsync({
          path: { organizationId, translationMemoryId },
          content: {
            'application/json': {
              sourceText: sourceText.trim(),
              targetLanguageTag: target.languageTag,
              targetText: target.text.trim(),
            },
          },
        });
      }
      messageService.success(
        <T
          keyName="translation_memory_entry_created"
          defaultValue="Entry created"
        />
      );
      onFinished();
    } catch {
      messageService.error('Failed to create entries');
    } finally {
      setSaving(false);
    }
  };

  const sourceLang = languageInfo[sourceLanguageTag];
  const sourceFlag = sourceLang?.flags?.[0] || '';
  const sourceName = sourceLang?.englishName || sourceLanguageTag;

  return (
    <Dialog
      data-cy="tm-create-entry-dialog"
      open={open}
      onClose={onClose}
      maxWidth="sm"
      fullWidth
    >
      <DialogTitle>
        <T
          keyName="translation_memory_create_entry_title"
          defaultValue="New entry"
        />
      </DialogTitle>
      <DialogContent sx={{ display: 'grid', gap: 2, pt: '8px !important' }}>
        <div>
          <StyledLangLabel>
            <FlagImage flagEmoji={sourceFlag} height={14} />
            {sourceName} ({t('translation_memory_source', 'source')})
          </StyledLangLabel>
          <TextField
            value={sourceText}
            onChange={(e) => setSourceText(e.target.value)}
            multiline
            minRows={2}
            fullWidth
            placeholder={t(
              'translation_memory_entry_source_placeholder',
              'Source text...'
            )}
            autoFocus
            data-cy="tm-entry-source-text"
          />
        </div>

        {targets.map((target, index) => {
          const selectableTags = availableLanguages.filter(
            (tag) => tag === target.languageTag || !usedTags.includes(tag)
          );

          return (
            <div key={index}>
              <StyledLangRow>
                <Box flex={1}>
                  <StyledLangLabel>
                    <Select
                      value={target.languageTag}
                      onChange={(e) =>
                        changeLanguage(index, e.target.value as string)
                      }
                      variant="standard"
                      size="small"
                      disableUnderline
                      sx={{ fontSize: 13 }}
                    >
                      {selectableTags.map((tag) => {
                        const li = languageInfo[tag];
                        const liFlag = li?.flags?.[0] || '';
                        return (
                          <MenuItem key={tag} value={tag}>
                            <Box display="flex" alignItems="center" gap={1}>
                              <FlagImage flagEmoji={liFlag} height={14} />
                              {li?.englishName || tag}
                            </Box>
                          </MenuItem>
                        );
                      })}
                    </Select>
                  </StyledLangLabel>
                  <TextField
                    value={target.text}
                    onChange={(e) => updateTarget(index, e.target.value)}
                    multiline
                    minRows={2}
                    fullWidth
                    placeholder={t(
                      'translation_memory_entry_target_placeholder',
                      'Translation...'
                    )}
                    data-cy="tm-entry-target-text"
                  />
                </Box>
                {targets.length > 1 && (
                  <IconButton
                    size="small"
                    onClick={() => removeTarget(index)}
                    sx={{ mt: 3 }}
                    aria-label={t(
                      'translation_memory_remove_language',
                      'Remove language'
                    )}
                  >
                    <XClose width={16} height={16} />
                  </IconButton>
                )}
              </StyledLangRow>
            </div>
          );
        })}

        {canAddMore && (
          <Button
            size="small"
            startIcon={<Plus width={14} height={14} />}
            onClick={addTarget}
            data-cy="tm-entry-add-language"
          >
            {t('translation_memory_entry_add_language', 'Add language')}
          </Button>
        )}

        <StyledActions>
          <Button onClick={onClose}>{t('global_cancel_button')}</Button>
          <LoadingButton
            onClick={handleSave}
            color="primary"
            variant="contained"
            loading={saving}
            disabled={!canSave}
            data-cy="tm-entry-create-submit"
          >
            {t('global_form_create')}
          </LoadingButton>
        </StyledActions>
      </DialogContent>
    </Dialog>
  );
};
