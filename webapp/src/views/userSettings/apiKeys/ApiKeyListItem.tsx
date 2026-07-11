import { useApiMutation } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { confirmation } from 'tg.hooks/confirmation';
import { T } from '@tolgee/react';
import { Box, Button, styled } from '@mui/material';
import { Link } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useMessage } from 'tg.hooks/useSuccessMessage';
import { Edit02 } from '@untitled-ui/icons-react';
import { ApiKeyExpiryInfo } from './ApiKeyExpiryInfo';
import { NewApiKeyInfo } from './NewApiKeyInfo';

const StyledRoot = styled(Box)`
  display: grid;
  gap: 0.5rem;

  @container (min-width: 899px) {
    // prettier-ignore
    grid-template-columns: minmax(200px, 1fr) minmax(0, 1fr) auto auto;
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

const StyledScopes = styled(Box)`
  font-style: italic;
  font-size: 12px;
  grid-column: 2 / span 3;
  text-align: right;
`;

const StyledProjectLink = styled(Link)`
  font-style: italic;
  text-decoration: none;
  color: ${({ theme }) => theme.palette.primary.main};
`;

export type NewApiKeyType = 'regenerated' | 'created' | undefined;

export const ApiKeyListItem = (props: {
  apiKey: components['schemas']['ApiKeyModel'];
  newTokenValue?: string;
  newTokenType?: NewApiKeyType;
}) => {
  const message = useMessage();

  const deleteMutation = useApiMutation({
    url: '/v2/api-keys/{apiKeyId}',
    method: 'delete',
    invalidatePrefix: '/v2/api-keys',
    options: {
      onSuccess: () => message.success(<T keyName="api-key-deleted-message" />),
    },
  });

  const onDelete = () => {
    confirmation({
      confirmButtonText: <T keyName="api-key-delete-button" />,
      message: (
        <Box>
          <Box sx={{ mb: 2 }}>
            <T keyName="api-key-delete-token-confirmation-message" />
          </Box>
          <b>{props.apiKey.description}</b>
        </Box>
      ),
      onConfirm: () =>
        deleteMutation.mutate({ path: { apiKeyId: props.apiKey.id } }),
    });
  };

  return (
    <StyledRoot data-cy="api-key-list-item">
      <Box>
        <StyledDescription
          data-cy="api-key-list-item-description"
          to={LINKS.USER_API_KEYS_EDIT.build({
            [PARAMS.API_KEY_ID]: props.apiKey.id,
          })}
        >
          {props.apiKey.description}
          <Edit02 className="edit-icon" style={{ width: '15px' }} />
        </StyledDescription>
        <StyledProjectLink
          to={LINKS.PROJECT.build({
            [PARAMS.PROJECT_ID]: props.apiKey.projectId,
          })}
        >
          {props.apiKey.projectName}
        </StyledProjectLink>
      </Box>

      <Box data-cy="api-key-list-item-last-used" sx={{ textAlign: 'right' }}>
        {props.apiKey.lastUsedAt ? (
          <T
            keyName="api-key-list-item-last-used"
            params={{ date: props.apiKey.lastUsedAt }}
          />
        ) : (
          <T keyName="api-key-list-never-used" />
        )}
      </Box>
      <Box>
        <Button
          data-cy="api-key-list-item-regenerate-button"
          size="small"
          variant="outlined"
          color="secondary"
          component={Link}
          to={LINKS.USER_API_KEYS_REGENERATE.build({
            [PARAMS.API_KEY_ID]: props.apiKey.id,
          })}
        >
          <T keyName="api-key-list-item-regenerate" />
        </Button>
      </Box>
      <Box>
        <Button
          data-cy="api-key-list-item-delete-button"
          size="small"
          variant="outlined"
          color="error"
          onClick={() => onDelete()}
        >
          <T keyName="api-key-list-item-delete" />
        </Button>
      </Box>
      {props.newTokenValue && (
        <NewApiKeyInfo
          newTokenType={props.newTokenType}
          newTokenValue={props.newTokenValue}
        />
      )}
      <ApiKeyExpiryInfo apiKey={props.apiKey} />
      <StyledScopes>{props.apiKey.scopes.join(', ')}</StyledScopes>
    </StyledRoot>
  );
};
