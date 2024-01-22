import { ComponentProps, FC } from 'react';
import { styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import SearchField from 'tg.component/common/form/fields/SearchField';

const StyledSearchField = styled(SearchField)`
  background-color: ${({ theme }) => theme.palette.background.default};
  transition: width 0.1s ease-in-out;
  width: 250px;
`;

export const SecondaryBarSearchField: FC<ComponentProps<typeof SearchField>> = (
  props
) => {
  const { t } = useTranslate();

  return (
    <StyledSearchField
      data-cy="global-list-search"
      placeholder={t('standard_search_label')}
      label={null}
      hiddenLabel={true}
      variant={'outlined'}
      size="small"
      {...props}
    />
  );
};
