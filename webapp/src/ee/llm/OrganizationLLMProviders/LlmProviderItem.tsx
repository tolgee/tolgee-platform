import { Box, IconButton, Menu, MenuItem, styled } from '@mui/material';
import { useRef, useState } from 'react';
import { ProviderItem } from './llmProvidersViewItems';
import { DotsVertical } from '@untitled-ui/icons-react';
import { T, useTranslate } from '@tolgee/react';
import { confirmation } from 'tg.hooks/confirmation';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { messageService } from 'tg.service/MessageService';
import { useLlmProviderTranslation } from 'tg.translationTools/useLlmProviderTranslation';

const StyledContainer = styled('div')`
  display: contents;
  &:not(:last-of-type) > * {
    border-bottom: 1px solid ${({ theme }) => theme.palette.divider};
  }
  &:hover > * {
    background: ${({ theme }) => theme.palette.tokens.text._states.hover};
  }
`;

const StyledItem = styled(Box)`
  display: flex;
  align-items: center;
  align-self: stretch;
  justify-self: stretch;
  gap: 8px;
  color: ${({ theme }) => theme.palette.text.primary};
  text-decoration: none;
`;

const StyledName = styled(StyledItem)`
  padding: 8px 16px;
`;

const StyledModel = styled(StyledItem)`
  padding: 8px 16px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledAction = styled(StyledItem)`
  color: ${({ theme }) => theme.palette.text.secondary};
  padding-right: 8px;
`;

type Props = {
  provider: ProviderItem;
  onEdit?: () => void;
};

export const LlmProviderItem = ({ provider, onEdit }: Props) => {
  const isEditable = provider.id !== undefined;
  const translateProviderType = useLlmProviderTranslation();
  const organization = useOrganization();
  const { t } = useTranslate();

  const [menuOpen, setMenuOpen] = useState(false);
  const actionProps = isEditable
    ? {
        onClick: onEdit,
        sx: { cursor: 'pointer' },
      }
    : null;
  const buttonRef = useRef<HTMLButtonElement>(null);

  const deleteLoadable = useApiMutation({
    url: '/v2/organizations/{organizationId}/llm-providers/{providerId}',
    method: 'delete',
    invalidatePrefix: '/v2/organizations/{organizationId}/llm-providers',
  });

  function handleDelete() {
    confirmation({
      title: <T keyName="llm_provider_delete_title" />,
      message: <T keyName="llm_provider_delete_message" />,
      onConfirm() {
        deleteLoadable.mutate(
          {
            path: {
              organizationId: organization!.id,
              providerId: provider.id!,
            },
          },
          {
            onSuccess() {
              messageService.success(
                <T keyName="llm_provider_delete_success" />
              );
            },
          }
        );
      },
    });
  }

  return (
    <StyledContainer>
      <StyledName {...actionProps} data-cy="llm-provider-item-name">
        {provider.name}
      </StyledName>
      <StyledModel
        {...actionProps}
        data-cy="llm-provider-item-type"
        data-cy-name={provider.name}
      >
        {translateProviderType(provider.type)}
      </StyledModel>
      {isEditable && (
        <StyledAction
          sx={{ pr: 1 }}
          data-cy="llm-provider-item-menu"
          data-cy-name={provider.name}
        >
          <IconButton
            size="small"
            color="inherit"
            ref={buttonRef}
            onClick={() => setMenuOpen(true)}
          >
            <DotsVertical />
          </IconButton>
        </StyledAction>
      )}
      {menuOpen && (
        <Menu
          anchorEl={buttonRef.current}
          open
          onClose={() => setMenuOpen(false)}
        >
          <MenuItem onClick={onEdit} data-cy="llm-provider-menu-item-edit">
            {t('llm_provider_edit')}
          </MenuItem>
          <MenuItem
            onClick={handleDelete}
            data-cy="llm-provider-menu-item-delete"
          >
            {t('llm_provider_delete')}
          </MenuItem>
        </Menu>
      )}
    </StyledContainer>
  );
};
