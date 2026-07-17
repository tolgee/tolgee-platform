import { IconButton } from '@mui/material';
import { XClose } from '@untitled-ui/icons-react';

import { stopAndPrevent } from 'tg.fixtures/eventHandler';

import { SearchSyntaxHelp } from './SearchSyntaxHelp';
import { useSearchEditor } from './useSearchEditor';
import {
  StyledEditorArea,
  StyledRoot,
  StyledSearchIcon,
} from './searchFieldStyles';

type Props = {
  value: string;
  onSearchChange: (value: string) => void;
  setSearchOpen?: (open: boolean) => void;
  languageTags: string[];
  placeholder?: string;
  className?: string;
  style?: React.CSSProperties;
};

export const TranslationsSearchField = (props: Props) => {
  const {
    value,
    onSearchChange,
    setSearchOpen,
    languageTags,
    placeholder,
    className,
    style,
  } = props;

  const editorAreaRef = useSearchEditor({
    value,
    onSearchChange,
    setSearchOpen,
    languageTags,
    placeholder,
  });

  return (
    <StyledRoot
      className={className}
      style={style}
      data-cy="global-search-field"
    >
      <StyledSearchIcon width={20} height={20} />
      <StyledEditorArea ref={editorAreaRef} />
      {Boolean(value) && (
        <IconButton
          data-cy="global-search-field-clear"
          size="small"
          onClick={stopAndPrevent(() => onSearchChange(''))}
          onMouseDown={stopAndPrevent()}
        >
          <XClose width={20} height={20} />
        </IconButton>
      )}
      <SearchSyntaxHelp languageTags={languageTags} />
    </StyledRoot>
  );
};
