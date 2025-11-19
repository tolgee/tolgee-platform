import React, { useEffect, useRef, useState } from 'react';
import {
  Box,
  Card,
  Chip,
  Stack,
  Tab,
  Tabs,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Tooltip,
  Typography,
  useTheme,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import {
  Bold01,
  Code01,
  Eye,
  Italic01,
  Underline01,
} from '@untitled-ui/icons-react';
import { components } from 'tg.service/billingApiSchema.generated';

export type TemplatePlaceholder =
  components['schemas']['EmailPlaceholderModel'];

type Props = {
  value: string;
  onChange: (value: string) => void;
  label?: string;
  disabled?: boolean;
  readOnly?: boolean;
  placeholders?: TemplatePlaceholder[];
};

type Mode = 'html' | 'preview';

export const HtmlTemplateEditor: React.FC<Props> = ({
  value,
  onChange,
  label,
  disabled,
  readOnly,
  placeholders = [],
}) => {
  const { t } = useTranslate();
  const theme = useTheme();
  const [mode, setMode] = useState<Mode>('html');
  const textareaRef = useRef<HTMLTextAreaElement>(null);
  const previewRef = useRef<HTMLDivElement>(null);

  const isDark = theme.palette.mode === 'dark';

  useEffect(() => {
    if (
      mode === 'preview' &&
      previewRef.current &&
      previewRef.current.innerHTML !== value
    ) {
      previewRef.current.innerHTML = value ?? '';
    }
  }, [mode, value]);

  const updateValue = (newValue: string) => {
    if (readOnly || disabled) return;
    onChange(newValue);
  };

  const getSelection = () => {
    const el = textareaRef.current;
    if (!el) return null;
    const start = el.selectionStart ?? 0;
    const end = el.selectionEnd ?? 0;
    return { start, end, value: el.value };
  };

  const wrapOrUnwrapSelection = (tag: 'b' | 'i' | 'u') => {
    if (mode === 'preview') {
      document.execCommand(
        tag === 'u' ? 'underline' : tag === 'i' ? 'italic' : 'bold',
        false
      );
      if (previewRef.current) {
        updateValue(previewRef.current.innerHTML);
      }
      return;
    }

    if (readOnly || disabled) return;
    const selection = getSelection();
    if (!selection) return;
    const { start, end, value: currentValue } = selection;
    const selected = currentValue.slice(start, end);
    const open = `<${tag}>`;
    const close = `</${tag}>`;

    const beforeSel = currentValue.slice(0, start);
    const afterSel = currentValue.slice(end);
    const selectedLower = selected.toLowerCase();
    const openLower = open.toLowerCase();
    const closeLower = close.toLowerCase();

    let newValue: string;
    let newPos = start;

    if (
      selectedLower.startsWith(openLower) &&
      selectedLower.endsWith(closeLower)
    ) {
      const inner = selected.slice(open.length, selected.length - close.length);
      newValue = beforeSel + inner + afterSel;
      newPos = start;
    } else if (
      beforeSel.toLowerCase().endsWith(openLower) &&
      afterSel.toLowerCase().startsWith(closeLower)
    ) {
      const trimmedBefore = beforeSel.slice(0, beforeSel.length - open.length);
      const trimmedAfter = afterSel.slice(close.length);
      newValue = trimmedBefore + selected + trimmedAfter;
      newPos = start - open.length;
    } else {
      newValue = beforeSel + open + selected + close + afterSel;
      newPos = start + open.length + selected.length + close.length;
    }

    updateValue(newValue);
    requestAnimationFrame(() => {
      if (textareaRef.current) {
        textareaRef.current.focus();
        textareaRef.current.selectionStart = newPos;
        textareaRef.current.selectionEnd = newPos;
      }
    });
  };

  const handleTabChange = (_: React.SyntheticEvent, newValue: Mode) => {
    setMode(newValue);
  };

  const insertPlaceholder = (placeholder: string) => {
    if (mode === 'preview') {
      document.execCommand('insertHTML', false, placeholder);
      if (previewRef.current) {
        updateValue(previewRef.current.innerHTML);
      }
      return;
    }
    const selection = getSelection();
    if (!selection) return;
    const { start, end, value: currentValue } = selection;
    const newValue = `${currentValue.slice(
      0,
      start
    )}${placeholder}${currentValue.slice(end)}`;
    updateValue(newValue);
    requestAnimationFrame(() => {
      if (textareaRef.current) {
        const pos = start + placeholder.length;
        textareaRef.current.selectionStart = pos;
        textareaRef.current.selectionEnd = pos;
        textareaRef.current.focus();
      }
    });
  };

  return (
    <Card variant="outlined">
      <Box display="grid" gap={1.5} px={2} pb={2}>
        <Box display="flex" justifyContent="space-between" alignItems="center">
          {label && (
            <Typography variant="subtitle1" fontWeight={600}>
              {label}
            </Typography>
          )}
          <Tabs
            value={mode}
            onChange={handleTabChange}
            aria-label="html-editor-mode"
            textColor="primary"
            indicatorColor="primary"
          >
            <Tab
              value="html"
              label={
                <Stack direction="row" spacing={0.5} alignItems="center">
                  <Code01 width={16} height={16} />
                  <span>{t('html_editor_source_label')}</span>
                </Stack>
              }
            />
            <Tab
              value="preview"
              label={
                <Stack direction="row" spacing={0.5} alignItems="center">
                  <Eye width={16} height={16} />
                  <span>{t('html_editor_preview_label')}</span>
                </Stack>
              }
            />
          </Tabs>
        </Box>

        <ToggleButtonGroup size="small" exclusive>
          <ToggleButton
            value="bold"
            onClick={() => wrapOrUnwrapSelection('b')}
            disabled={disabled || readOnly}
          >
            <Bold01 width={16} height={16} />
          </ToggleButton>
          <ToggleButton
            value="italic"
            onClick={() => wrapOrUnwrapSelection('i')}
            disabled={disabled || readOnly}
          >
            <Italic01 width={16} height={16} />
          </ToggleButton>
          <ToggleButton
            value="underline"
            onClick={() => wrapOrUnwrapSelection('u')}
            disabled={disabled || readOnly}
          >
            <Underline01 width={16} height={16} />
          </ToggleButton>
        </ToggleButtonGroup>

        {mode === 'html' ? (
          <TextField
            inputRef={textareaRef}
            label={t('html_editor_source_label')}
            multiline
            minRows={8}
            value={value}
            onChange={(e) => updateValue(e.target.value)}
            disabled={disabled}
            InputProps={{
              readOnly,
              sx: {
                fontFamily: 'Source Code Pro, monospace',
                backgroundColor: isDark
                  ? 'rgba(255,255,255,0.05)'
                  : 'background.paper',
                color: isDark ? '#fff' : 'inherit',
              },
            }}
          />
        ) : (
          <Box
            ref={previewRef}
            contentEditable={!readOnly && !disabled}
            suppressContentEditableWarning
            sx={{
              minHeight: 150,
              border: 1,
              borderColor: 'divider',
              borderRadius: 1,
              padding: 1,
              fontFamily: 'inherit',
              backgroundColor: isDark
                ? 'rgba(255,255,255,0.05)'
                : 'background.paper',
              color: isDark ? '#fff' : 'inherit',
              '&:focus': { outline: 'none' },
            }}
            onInput={() => {
              if (previewRef.current) {
                updateValue(previewRef.current.innerHTML);
              }
            }}
          />
        )}

        {!!placeholders.length && (
          <Box>
            <Typography variant="caption" color="text.secondary">
              {t('html_editor_placeholders_label')}
            </Typography>
            <Stack direction="row" flexWrap="wrap" gap={1}>
              {placeholders.map((ph) => {
                const chip = (
                  <Chip
                    key={ph.placeholder}
                    label={`${ph.placeholder} – ${ph.description}`}
                    size="small"
                    onClick={() => insertPlaceholder(ph.placeholder)}
                    clickable={!disabled && !readOnly}
                    disabled={disabled || readOnly}
                  />
                );
                if (!ph.exampleValue) {
                  return chip;
                }
                return (
                  <Tooltip
                    key={ph.placeholder}
                    title={
                      <T
                        keyName={'html_editor_example'}
                        params={{ value: ph.exampleValue }}
                      />
                    }
                  >
                    {chip}
                  </Tooltip>
                );
              })}
            </Stack>
          </Box>
        )}
      </Box>
    </Card>
  );
};
