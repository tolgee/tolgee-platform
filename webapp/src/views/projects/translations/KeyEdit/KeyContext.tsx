import { Box, styled, Typography, Link as MuiLink } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import React from 'react';
import { Link } from 'react-router-dom';

import { BoxLoading } from 'tg.component/common/BoxLoading';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { queryEncode } from 'tg.hooks/useUrlSearchState';
import { useApiQuery } from 'tg.service/http/useQueryApi';

const StyledContent = styled(Box)`
  display: grid;
  justify-items: start;
  gap: 3px 16px;
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  border-radius: 4px;
  padding: 10px;
  grid-template-columns: 1fr 1fr;
`;

const StyledReference = styled(MuiLink)`
  display: block;
  background: ${({ theme }) => theme.palette.emphasis[50]};
  max-height: 1.5em;
  padding: 0px 4px;
  border-radius: 4px;
  border: 1px solid ${({ theme }) => theme.palette.emphasis[100]};
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
` as typeof MuiLink;

const StyledReferencePrefix = styled('span')`
  margin-right: 4px;
  padding-right: 4px;
  border-right: 1px solid ${({ theme }) => theme.palette.emphasis[100]};
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

const StyledReferenceText = styled('span')`
  flex-shrink: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

const StyledTranslation = styled('div')`
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 100%;
`;

type Props = {
  keyId: number;
};

export const KeyContext = ({ keyId }: Props) => {
  const project = useProject();
  const { t } = useTranslate();

  const contextLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/keys/{id}/big-meta',
    method: 'get',
    path: { projectId: project.id, id: keyId },
  });

  if (contextLoadable.isLoading) {
    return <BoxLoading />;
  }

  return (
    <>
      <Box>
        <Typography fontSize={15}>
          {t('key_edit_modal_context_hint')}
        </Typography>
      </Box>
      <StyledContent>
        {contextLoadable.data?._embedded?.keys?.map(
          ({ id, name, namespace, baseTranslation }) => (
            <React.Fragment key={id}>
              <StyledReference
                key={id}
                component={Link}
                target="_blank"
                rel="noreferrer noopener"
                to={
                  LINKS.PROJECT_TRANSLATIONS_SINGLE.build({
                    [PARAMS.PROJECT_ID]: project.id,
                  }) +
                  queryEncode({
                    id,
                  })
                }
              >
                {namespace && (
                  <StyledReferencePrefix>{namespace}</StyledReferencePrefix>
                )}
                <StyledReferenceText>{name}</StyledReferenceText>
              </StyledReference>
              <StyledTranslation>{baseTranslation}</StyledTranslation>
            </React.Fragment>
          )
        )}
      </StyledContent>
    </>
  );
};
