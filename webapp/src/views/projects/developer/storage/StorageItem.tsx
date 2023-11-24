import { useState } from 'react';
import { Box, Button, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';
import { StorageEditDialog } from './StorageEditDialog';

type ContentStorageModel = components['schemas']['ContentStorageModel'];

const StyledContainer = styled('div')`
  display: flex;
  padding: 8px 16px;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 10px;
  & + & {
    border-top: 1px solid ${({ theme }) => theme.palette.divider};
  }
`;

type Props = {
  data: ContentStorageModel;
};

export const StorageItem = ({ data }: Props) => {
  const { t } = useTranslate();
  const [formOpen, setFormOpen] = useState(false);

  return (
    <StyledContainer data-cy="storage-list-item" data-cy-name={data.name}>
      <Box display="flex" gap={2} alignItems="center">
        <div>{data.name}</div>
      </Box>
      <Box
        display="flex"
        gap={2}
        alignItems="center"
        justifyContent="end"
        flexGrow={1}
      >
        <div>{data.publicUrlPrefix}</div>
        <Button
          size="small"
          onClick={() => setFormOpen(true)}
          data-cy="storage-item-edit"
        >
          {t('storage_item_edit')}
        </Button>
      </Box>
      {formOpen && (
        <StorageEditDialog onClose={() => setFormOpen(false)} data={data} />
      )}
    </StyledContainer>
  );
};
