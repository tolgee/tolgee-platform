import React, { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Checkbox,
  Dialog,
  DialogContent,
  DialogTitle,
  ListItemText,
  MenuItem,
  Select,
  styled,
  TextField,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { languageInfo } from '@tginternal/language-util/lib/generated/languageInfo';
import { LanguageHeading } from 'tg.component/languages/LanguageHeading';

const StyledActions = styled('div')`
  display: flex;
  gap: 8px;
  padding-top: 16px;
  justify-content: end;
`;

// 14px label that hosts the LanguageHeading (flag + name with bold-when-base) so
// the dialog labels match the language headers in the Translations view. The wrapper
// only supplies font size, color and bottom margin — bold weight is owned by
// LanguageHeading via language.base.
const StyledLangLabel = styled('div')`
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: ${({ theme }) => theme.palette.text.secondary};
  margin-bottom: 4px;
`;

const StyledDialogTitle = styled(DialogTitle)`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
`;

type Props = {
  open: boolean;
  onClose: () => void;
  /** Receives the target language tags of the just-created entry so the caller can make
   *  sure those languages stay visible in the list. */
  onFinished: (createdLanguageTags: string[]) => void;
  organizationId: number;
  translationMemoryId: number;
  sourceLanguageTag: string;
  // Full org language tag list (minus source). The header multi-select offers any of these
  // as a target.
  allLanguageTags: string[];
  // Up-to-2 tags to pre-select. Empty array → dialog falls back to the first 2 of
  // `allLanguageTags`.
  initialSelectedTags: string[];
};

const defaultPreselection = (initial: string[], all: string[]) =>
  (initial.length > 0 ? initial : all).slice(0, 2);

export const TranslationMemoryCreateEntryDialog: React.VFC<Props> = ({
  open,
  onClose,
  onFinished,
  organizationId,
  translationMemoryId,
  sourceLanguageTag,
  allLanguageTags,
  initialSelectedTags,
}) => {
  const { t } = useTranslate();
  const [sourceText, setSourceText] = useState('');
  const [selectedTags, setSelectedTags] = useState<string[]>(() =>
    defaultPreselection(initialSelectedTags, allLanguageTags)
  );
  // Keyed by lang tag so toggling a lang off and back on within one dialog session keeps
  // whatever the user typed. The save path filters to `selectedTags` so unselected text is
  // dropped on submit.
  const [textByTag, setTextByTag] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);

  // If the dialog mounts before org languages finish loading, `allLanguageTags` is empty
  // and `selectedTags` initialises to []. Seed once languages arrive so the user has at
  // least one input. Empty selection should never be the steady state on open.
  useEffect(() => {
    if (selectedTags.length === 0 && allLanguageTags.length > 0) {
      setSelectedTags(
        defaultPreselection(initialSelectedTags, allLanguageTags)
      );
    }
  }, [allLanguageTags, initialSelectedTags, selectedTags.length]);

  const createMultipleMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/translation-memories/{translationMemoryId}/entries/multiple',
    method: 'post',
    invalidatePrefix: '/v2/organizations/{organizationId}/translation-memories',
  });

  const updateText = (tag: string, text: string) => {
    setTextByTag((prev) => ({ ...prev, [tag]: text }));
  };

  const canSave =
    sourceText.trim().length > 0 &&
    selectedTags.some((tag) => textByTag[tag]?.trim()) &&
    !saving;

  const handleSave = async () => {
    const translations = selectedTags
      .map((tag) => ({
        targetLanguageTag: tag,
        targetText: textByTag[tag]?.trim() ?? '',
      }))
      .filter((entry) => entry.targetText.length > 0);
    if (!sourceText.trim() || translations.length === 0) return;

    setSaving(true);
    try {
      await createMultipleMutation.mutateAsync({
        path: { organizationId, translationMemoryId },
        content: {
          'application/json': {
            sourceText: sourceText.trim(),
            translations,
          },
        },
      });
      messageService.success(
        <T
          keyName="translation_memory_entry_created"
          defaultValue="Entry created"
        />
      );
      onFinished(translations.map((tr) => tr.targetLanguageTag));
    } catch {
      messageService.error(
        <T
          keyName="translation_memory_create_entry_error"
          defaultValue="Failed to create entries"
        />
      );
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
      <StyledDialogTitle>
        <T
          keyName="translation_memory_create_entry_title"
          defaultValue="New entry"
        />
        <Select
          multiple
          size="small"
          value={selectedTags}
          onChange={(e) => {
            const next = e.target.value;
            setSelectedTags(typeof next === 'string' ? next.split(',') : next);
          }}
          displayEmpty
          data-cy="tm-entry-language-multiselect"
          sx={{ minWidth: 180, fontSize: 14 }}
          renderValue={(selected) => {
            if (selected.length === 0) {
              return t(
                'translation_memory_entry_languages_none',
                'No languages'
              );
            }
            return t(
              'translation_memory_entry_languages_count',
              '{count, plural, one {# language} other {# languages}}',
              { count: selected.length }
            );
          }}
        >
          {allLanguageTags.map((tag) => {
            const li = languageInfo[tag];
            return (
              <MenuItem
                key={tag}
                value={tag}
                data-cy="tm-entry-language-multiselect-item"
              >
                <Checkbox checked={selectedTags.includes(tag)} size="small" />
                <ListItemText
                  primary={
                    <LanguageHeading
                      language={{
                        name: li?.englishName || tag,
                        flagEmoji: li?.flags?.[0] || '',
                      }}
                    />
                  }
                />
              </MenuItem>
            );
          })}
        </Select>
      </StyledDialogTitle>
      <DialogContent sx={{ display: 'grid', gap: 2, pt: '8px !important' }}>
        <div>
          <StyledLangLabel>
            <LanguageHeading
              language={{ name: sourceName, flagEmoji: sourceFlag, base: true }}
            />
          </StyledLangLabel>
          <TextField
            value={sourceText}
            onChange={(e) => setSourceText(e.target.value)}
            multiline
            minRows={2}
            fullWidth
            autoFocus
            data-cy="tm-entry-source-text"
          />
        </div>

        {selectedTags.map((tag) => {
          const li = languageInfo[tag];
          return (
            <div key={tag}>
              <Box>
                <StyledLangLabel>
                  <LanguageHeading
                    language={{
                      name: li?.englishName || tag,
                      flagEmoji: li?.flags?.[0] || '',
                    }}
                  />
                </StyledLangLabel>
                <TextField
                  value={textByTag[tag] ?? ''}
                  onChange={(e) => updateText(tag, e.target.value)}
                  multiline
                  minRows={2}
                  fullWidth
                  data-cy="tm-entry-target-text"
                />
              </Box>
            </div>
          );
        })}

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
