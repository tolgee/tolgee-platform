import { ComponentProps, useRef, useState } from 'react';
import {
  ListItemButton,
  ListItemText,
  Paper,
  Popper,
  styled,
} from '@mui/material';
import { T } from '@tolgee/react';

import { HeaderSearchField } from 'tg.component/layout/HeaderSearchField';

import {
  applySuggestion,
  getSuggestions,
  SuggestionItem,
} from './searchSuggestions';

const StyledPaper = styled(Paper)`
  margin-top: 4px;
  max-height: 320px;
  overflow-y: auto;
`;

const StyledQualifier = styled('span')`
  font-family: monospace;
  color: ${({ theme }) => theme.palette.primary.main};
`;

const FIELD_HINTS = {
  'key:': (
    <T
      keyName="translations_search_suggestion_key"
      defaultValue="Search only in key names"
    />
  ),
  'description:': (
    <T
      keyName="translations_search_help_description"
      defaultValue="Search only in key descriptions"
    />
  ),
  'namespace:': (
    <T
      keyName="translations_search_help_namespace"
      defaultValue="Search only in namespaces"
    />
  ),
  'translation:': (
    <T
      keyName="translations_search_help_translation"
      defaultValue="Search only in translation texts"
    />
  ),
} as Record<string, React.ReactNode>;

function suggestionHint(item: SuggestionItem) {
  if (item.kind === 'language') {
    return (
      <T
        keyName="translations_search_help_language"
        defaultValue="Search only in one language"
      />
    );
  }
  return FIELD_HINTS[item.insert];
}

type Props = ComponentProps<typeof HeaderSearchField> & {
  languageTags: string[];
  className?: string;
};

export const TranslationsSearchField = (props: Props) => {
  const {
    languageTags,
    value,
    onSearchChange,
    setSearchOpen,
    className,
    ...rest
  } = props;
  const anchorRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const [caret, setCaret] = useState(0);
  const [focused, setFocused] = useState(false);
  const [dismissed, setDismissed] = useState(false);
  const [highlighted, setHighlighted] = useState(0);

  const suggestions = focused
    ? getSuggestions(value, caret, languageTags)
    : undefined;
  const open = Boolean(suggestions) && !dismissed;
  const items = suggestions?.items ?? [];
  const highlightedSafe = Math.min(highlighted, items.length - 1);

  const syncCaret = () => {
    setCaret(inputRef.current?.selectionStart ?? 0);
    setDismissed(false);
  };

  const accept = (item: SuggestionItem) => {
    if (!suggestions) {
      return;
    }
    const result = applySuggestion(value, suggestions, item);
    onSearchChange(result.value);
    setCaret(result.caret);
    setHighlighted(0);
    requestAnimationFrame(() => {
      inputRef.current?.focus();
      inputRef.current?.setSelectionRange(result.caret, result.caret);
    });
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (!open || items.length === 0) {
      return;
    }
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setHighlighted((highlightedSafe + 1) % items.length);
    }
    if (e.key === 'ArrowUp') {
      e.preventDefault();
      setHighlighted((highlightedSafe - 1 + items.length) % items.length);
    }
    if (e.key === 'Enter') {
      e.preventDefault();
      accept(items[highlightedSafe]);
    }
  };

  const handleKeyUp = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') {
      if (open) {
        setDismissed(true);
        return;
      }
      onSearchChange('');
      setSearchOpen?.(false);
      return;
    }
    syncCaret();
  };

  return (
    <div ref={anchorRef} className={className} style={{ display: 'grid' }}>
      <HeaderSearchField
        value={value}
        onSearchChange={(newValue) => {
          onSearchChange(newValue);
          syncCaret();
        }}
        setSearchOpen={setSearchOpen}
        inputRef={inputRef}
        onKeyDown={handleKeyDown}
        onKeyUp={handleKeyUp}
        onFocus={() => {
          setFocused(true);
          syncCaret();
        }}
        onBlur={() => setFocused(false)}
        onSelect={syncCaret}
        {...rest}
      />
      <Popper
        open={open}
        anchorEl={anchorRef.current}
        placement="bottom-start"
        style={{ zIndex: 1300 }}
      >
        <StyledPaper
          elevation={4}
          data-cy="translations-search-suggestions-popup"
          style={{ minWidth: anchorRef.current?.clientWidth }}
        >
          {items.map((item, index) => (
            <ListItemButton
              key={item.insert}
              dense
              selected={index === highlightedSafe}
              onMouseDown={(e) => {
                e.preventDefault();
                accept(item);
              }}
              data-cy="translations-search-suggestion-item"
            >
              <ListItemText
                primary={<StyledQualifier>{item.insert}</StyledQualifier>}
                secondary={suggestionHint(item)}
              />
            </ListItemButton>
          ))}
        </StyledPaper>
      </Popper>
    </div>
  );
};
