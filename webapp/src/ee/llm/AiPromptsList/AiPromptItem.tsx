import { Box, IconButton, Menu, MenuItem, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { DotsVertical } from '@untitled-ui/icons-react';
import { Link, useHistory } from 'react-router-dom';
import { useRef, useState } from 'react';

import { getAiPlaygroundUrl } from 'tg.constants/links';
import { components } from 'tg.service/apiSchema.generated';
import { confirmation } from 'tg.hooks/confirmation';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import { PromptRename } from '../AiPrompt/PromptRename';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';

type PromptModel = components['schemas']['PromptModel'];

const StyledContainer = styled('div')`
  display: contents;
  &:not(:last-of-type) > * {
    border-bottom: 1px solid ${({ theme }) => theme.palette.divider};
  }
  &:hover > * {
    background: ${({ theme }) => theme.palette.tokens.text._states.hover};
    cursor: pointer;
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
  prompt: PromptModel;
};

export const AiPromptItem = ({ prompt }: Props) => {
  const [menuOpen, setMenuOpen] = useState(false);
  const [renameOpen, setRenameOpen] = useState(false);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const { t } = useTranslate();
  const history = useHistory();
  const { satisfiesPermission } = useProjectPermissions();

  const deleteLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/prompts/{promptId}',
    method: 'delete',
    invalidatePrefix: '/v2/projects/{projectId}/prompts',
  });

  const linkProps = {
    component: Link,
    to: getAiPlaygroundUrl(prompt.projectId, prompt.id),
  };

  function handleRename() {
    setMenuOpen(false);
    setRenameOpen(true);
  }

  function handleDelete() {
    setMenuOpen(false);
    confirmation({
      message: (
        <T keyName="ai_prompts_delete_message" params={{ name: prompt.name }} />
      ),
      confirmButtonText: <T keyName="ai_prompt_delete_confirm" />,
      onConfirm() {
        deleteLoadable.mutate(
          {
            path: {
              projectId: prompt.projectId,
              promptId: prompt.id,
            },
          },
          {
            onSuccess() {
              messageService.success(<T keyName="ai_prompts_delete_success" />);
            },
          }
        );
      },
    });
  }

  return (
    <StyledContainer>
      <StyledName {...linkProps} data-cy="ai-prompt-item-name">
        {prompt.name}
      </StyledName>
      <StyledModel {...linkProps}>{prompt.providerName}</StyledModel>
      <StyledAction sx={{ pr: 1 }}>
        {satisfiesPermission('prompts.edit') && (
          <IconButton
            size="small"
            color="inherit"
            ref={buttonRef}
            onClick={() => setMenuOpen(true)}
            data-cy="ai-prompt-item-menu"
            data-cy-name={prompt.name}
          >
            <DotsVertical />
          </IconButton>
        )}
      </StyledAction>
      {menuOpen && (
        <Menu
          anchorEl={buttonRef.current}
          open
          onClose={() => setMenuOpen(false)}
        >
          <MenuItem onClick={() => history.push(linkProps.to)}>
            {t('ai_prompts_open_in_playground')}
          </MenuItem>
          <MenuItem
            onClick={handleRename}
            data-cy="ai-prompts-menu-item-rename"
          >
            {t('ai_prompts_rename')}
          </MenuItem>
          <MenuItem
            onClick={handleDelete}
            data-cy="ai-prompts-menu-item-delete"
          >
            {t('ai_prompts_delete')}
          </MenuItem>
        </Menu>
      )}
      {renameOpen && (
        <PromptRename
          data={prompt}
          onClose={() => setRenameOpen(false)}
          projectId={prompt.projectId}
        />
      )}
    </StyledContainer>
  );
};
