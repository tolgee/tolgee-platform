import { Popover } from '@mui/material';

import { SearchSelectContent } from './SearchSelectContent';

export const SearchSelectPopover: typeof SearchSelectContent = (props) => {
  const { anchorEl, open, onClose } = props;
  return (
    <Popover
      anchorEl={anchorEl}
      open={open}
      onClose={onClose}
      anchorOrigin={{
        vertical: 'top',
        horizontal: 'center',
      }}
      transformOrigin={{
        vertical: 'top',
        horizontal: 'center',
      }}
    >
      <SearchSelectContent {...props} />
    </Popover>
  );
};
