import React from 'react';
import { Box, styled } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { BatchIndicator } from './BatchIndicator';
import { useTranslate } from '@tolgee/react';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';

type BatchJobModel = components['schemas']['BatchJobModel'];

const StyledContainer = styled('div')`
  display: grid;
  grid-template-columns: auto auto auto;
  align-items: center;
  gap: 10px;
  padding: 15px;
`;

const StyledCell = styled(Box)`
  display: flex;
  align-items: center;
`;

type Props = {
  data: BatchJobModel[];
};

export const OperationsList = ({ data }: Props) => {
  const { t } = useTranslate();
  return (
    <StyledContainer>
      {data?.map((o) => (
        <React.Fragment key={o.id}>
          <StyledCell sx={{ whiteSpace: 'nowrap' }}>
            {t('batch_operation_progress', {
              totalItems: o.totalItems,
              progress: o.progress,
            })}
          </StyledCell>
          <StyledCell>
            <BatchIndicator data={o} />
          </StyledCell>
          <StyledCell>
            {o.author && (
              <AvatarImg
                owner={{
                  avatar: o.author.avatar,
                  id: o.author.id,
                  name: o.author.name,
                  type: 'USER',
                }}
                size={24}
              />
            )}
          </StyledCell>
        </React.Fragment>
      ))}
    </StyledContainer>
  );
};
