import { MenuProps, Menu } from '@material-ui/core';

import { components } from 'tg.service/apiSchema.generated';
import { getLanguagesContent } from './getLanguagesContent';

type LanguageModel = components['schemas']['LanguageModel'];

type Props = {
  anchorEl: MenuProps['anchorEl'];
  onClose: () => void;
  languages?: LanguageModel[];
  value?: string[];
  onChange: (languages: string[]) => void;
};

export const LanguagesMenu: React.FC<Props> = ({
  anchorEl,
  onClose,
  languages,
  value,
  onChange,
}) => {
  return (
    <Menu
      anchorEl={anchorEl}
      open={Boolean(anchorEl)}
      getContentAnchorEl={null}
      onClose={onClose}
      anchorOrigin={{
        vertical: 'bottom',
        horizontal: 'left',
      }}
    >
      {getLanguagesContent({
        languages: languages || [],
        value: value || [],
        onChange,
      })}
    </Menu>
  );
};
