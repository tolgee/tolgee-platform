import React from 'react';
import { Box, styled, useTheme } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { BatchIndicator } from './BatchIndicator';
import { useTolgee, useTranslate } from '@tolgee/react';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import { useBatchOperationTypeTranslate } from 'tg.translationTools/useBatchOperationTypeTranslation';
import { OperationAbortButton } from './OperationAbortButton';

type BatchJobModel = components['schemas']['BatchJobModel'];

const StyledContainer = styled('div')`
  display: grid;
  grid-template-columns: auto auto auto 1fr auto auto;
  align-items: center;
  padding: 15px;
  gap: 0 10px;
  min-width: 250px;
`;

const StyledCell = styled(Box)`
  margin: 5px 0px;
  display: flex;
  align-items: center;
  white-space: nowrap;
`;

type Props = {
  data: BatchJobModel[];
};

export const OperationsList = ({ data }: Props) => {
  const tolgee = useTolgee(['language']);
  const translateType = useBatchOperationTypeTranslate();
  const theme = useTheme();
  const { t } = useTranslate();

  return (
    <StyledContainer>
      {data?.map((o) => (
        <React.Fragment key={o.id}>
          <StyledCell>
            {Intl.DateTimeFormat(tolgee.getLanguage(), {
              timeStyle: 'short',
            }).format(o.updatedAt)}
          </StyledCell>
          <StyledCell>{translateType(o.type)}</StyledCell>
          <StyledCell>
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
          <StyledCell>
            <OperationAbortButton operation={o} />
          </StyledCell>
          {o.errorMessage && (
            <StyledCell
              sx={{ gridColumn: '1 / -1', mt: 0 }}
              color={theme.palette.error.main}
            >
              <TranslatedError code={o.errorMessage} />
            </StyledCell>
          )}
        </React.Fragment>
      ))}
    </StyledContainer>
  );
};
