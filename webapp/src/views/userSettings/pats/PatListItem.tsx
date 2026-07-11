import { components } from 'tg.service/apiSchema.generated';
import { Box, Button, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { confirmation } from 'tg.hooks/confirmation';
import { PatExpiryInfo } from './PatExpiryInfo';
import { LINKS, PARAMS } from 'tg.constants/links';
import { Link } from 'react-router-dom';
import { Edit02 } from '@untitled-ui/icons-react';
import { NewTokenInfo } from './NewTokenInfo';

const StyledRoot = styled(Box)`
  display: grid;
  gap: 0.5rem;

  @container (min-width: 899px) {
    grid-template-columns: 2fr auto auto auto;
  }

  border-bottom: 1px solid ${({ theme }) => theme.palette.emphasis.A100};
  align-items: center;

  &:last-child {
    border-bottom: none;
  }

  padding: 0.5rem;
`;

const StyledDescription = styled(Link)`
  font-weight: bold;
  display: flex;
  align-items: center;
  gap: 0.25rem;
  cursor: pointer;
  color: ${({ theme }) => theme.palette.text.primary};
  text-decoration: none;

  & .edit-icon {
    opacity: 0;
    transition: all 0.1s;
  }

  &:hover .edit-icon {
    opacity: 1;
  }
`;

export type NewTokenType = 'regenerated' | 'created' | undefined;

export function PatListItem(props: {
  pat: components['schemas']['PatModel'];
  newTokenValue?: string;
  newTokenType?: NewTokenType;
}) {
  const message = useMessage();

  const deleteMutation = useApiMutation({
    url: '/v2/pats/{id}',
    method: 'delete',
    invalidatePrefix: '/v2/pats',
    options: {
      onSuccess: () => message.success(<T keyName="pat-deleted-message" />),
    },
  });

  const onDelete = () => {
    confirmation({
      confirmButtonText: <T keyName="pat-delete-button" />,
      message: (
        <Box>
          <Box sx={{ mb: 2 }}>
            <T keyName="pat-delete-token-confirmation-message" />
          </Box>
          <b>{props.pat.description}</b>
        </Box>
      ),
      onConfirm: () => deleteMutation.mutate({ path: { id: props.pat.id } }),
    });
  };

  return (
    <StyledRoot data-cy="pat-list-item">
      <StyledDescription
        data-cy="pat-list-item-description"
        to={LINKS.USER_PATS_EDIT.build({
          [PARAMS.PAT_ID]: props.pat.id,
        })}
      >
        {props.pat.description}
        <Edit02 className="edit-icon" style={{ width: '15px' }} />
      </StyledDescription>
      <Box data-cy="pat-list-item-last-used">
        {props.pat.lastUsedAt ? (
          <T
            keyName="pat-list-item-last-used"
            params={{ date: props.pat.lastUsedAt }}
          />
        ) : (
          <T keyName="pat-list-never-used" />
        )}
      </Box>
      <Box>
        <Button
          data-cy="pat-list-item-regenerate-button"
          size="small"
          variant="outlined"
          color="secondary"
          component={Link}
          to={LINKS.USER_PATS_REGENERATE.build({
            [PARAMS.PAT_ID]: props.pat.id,
          })}
        >
          <T keyName="pat-list-item-regenerate" />
        </Button>
      </Box>
      <Box>
        <Button
          data-cy="pat-list-item-delete-button"
          size="small"
          variant="outlined"
          color="error"
          onClick={() => onDelete()}
        >
          <T keyName="pat-list-item-delete" />
        </Button>
      </Box>
      {props.newTokenValue && (
        <NewTokenInfo
          newTokenType={props.newTokenType}
          newTokenValue={props.newTokenValue}
        />
      )}
      <PatExpiryInfo pat={props.pat} />
    </StyledRoot>
  );
}
