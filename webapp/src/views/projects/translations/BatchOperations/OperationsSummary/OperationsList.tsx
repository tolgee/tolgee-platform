import React from 'react';
import { Box, Link as MuiLink, styled, useTheme } from '@mui/material';

import { components } from 'tg.service/apiSchema.generated';
import { BatchIndicator } from './BatchIndicator';
import { useTolgee, useTranslate } from '@tolgee/react';
import { AvatarImg } from 'tg.component/common/avatar/AvatarImg';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import { useBatchOperationTypeTranslate } from 'tg.translationTools/useBatchOperationTypeTranslation';
import { OperationAbortButton } from './OperationAbortButton';
import { LINKS, PARAMS, QUERY } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { Link } from 'react-router-dom';

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

const StyledLink = styled(MuiLink)`
  color: ${({ theme }) => theme.palette.text.primary};
  text-decoration: underline;
  cursor: pointer;
` as typeof MuiLink;

type Props = {
  data: BatchJobModel[];
};

export const OperationsList = ({ data }: Props) => {
  const tolgee = useTolgee(['language']);
  const project = useProject();
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
              sx={{
                gridColumn: '1 / -1',
                mt: 0,
                gap: 0.5,
              }}
            >
              <Box color={theme.palette.error.main}>
                <TranslatedError code={o.errorMessage} />
              </Box>
              <div>
                (
                <StyledLink
                  component={Link}
                  to={`${LINKS.PROJECT_TRANSLATIONS.build({
                    [PARAMS.PROJECT_ID]: project.id,
                  })}?${QUERY.TRANSLATIONS_PREFILTERS_FAILED_JOB}=${o.id}`}
                >
                  {t('batch_operation_show_failed_keys')}
                </StyledLink>
                )
              </div>
            </StyledCell>
          )}
        </React.Fragment>
      ))}
    </StyledContainer>
  );
};
