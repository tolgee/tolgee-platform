import { useRef, useState } from 'react';
import {
  Box,
  Button,
  ButtonGroup,
  Menu,
  MenuItem,
  styled,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { ArrowDropDown } from 'tg.component/CustomIcons';
import { messageService } from 'tg.service/MessageService';

import { PromptItem } from './PromptLoadMenu';
import { PromptSaveDialog } from './PromptSaveDialog';

type PromptModel = components['schemas']['PromptModel'];

const StyledArrowButton = styled(Button)`
  padding-left: 6px;
  padding-right: 6px;
  min-width: unset !important;
`;

type Props = {
  projectId: number;
  data: Omit<PromptModel, 'projectId' | 'id' | 'name'>;
  existingPrompt?: PromptItem;
  disabled?: boolean;
  unsavedChanges?: boolean;
  onSuccess: (prompt: PromptItem) => void;
};

export const PromptSaveMenu = ({
  projectId,
  data,
  existingPrompt,
  disabled,
  unsavedChanges,
  onSuccess,
}: Props) => {
  const [open, setOpen] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const { t } = useTranslate();

  const buttonRef = useRef<HTMLButtonElement>(null);

  const updatePrompt = useApiMutation({
    url: '/v2/projects/{projectId}/prompts/{promptId}',
    method: 'put',
    invalidatePrefix: '/v2/projects/{projectId}/prompts',
  });

  return (
    <Box>
      {typeof existingPrompt?.id === 'number' ? (
        <ButtonGroup variant="contained" size="small" color="primary">
          <LoadingButton
            disabled={disabled || !unsavedChanges}
            loading={updatePrompt.isLoading}
            data-cy="ai-prompt-save-button"
            onClick={() => {
              updatePrompt.mutate(
                {
                  path: { projectId, promptId: existingPrompt.id! },
                  content: {
                    'application/json': { ...data, name: existingPrompt.name },
                  },
                },
                {
                  onSuccess(data) {
                    messageService.success(
                      <T keyName="ai_prompt_update_success" />
                    );
                    onSuccess(data);
                  },
                }
              );
            }}
          >
            {t('ai_prompt_save_label')}
          </LoadingButton>
          <StyledArrowButton
            data-cy="ai-prompt-save-more-button"
            disabled={disabled}
            onClick={() => setOpen(true)}
            ref={buttonRef as any}
          >
            <ArrowDropDown />
          </StyledArrowButton>
        </ButtonGroup>
      ) : (
        <LoadingButton
          variant="contained"
          size="small"
          color="primary"
          ref={buttonRef}
          onClick={() => setCreateOpen(true)}
          disabled={disabled}
          data-cy="ai-prompt-save-as-new-button"
        >
          {t('ai_prompt_save_as_new_label')}
        </LoadingButton>
      )}
      {open && (
        <Menu
          open={true}
          onClose={() => setOpen(false)}
          anchorEl={buttonRef.current}
          MenuListProps={{ sx: { minWidth: 250 } }}
          anchorOrigin={{ horizontal: 'right', vertical: 'top' }}
          transformOrigin={{ horizontal: 'right', vertical: 'bottom' }}
        >
          <MenuItem
            onClick={() => {
              setOpen(false);
              setCreateOpen(true);
            }}
            data-cy="ai-prompt-save-as-new-button"
          >
            {t('ai_prompt_save_as_new_label')}
          </MenuItem>
        </Menu>
      )}
      {createOpen && (
        <PromptSaveDialog
          projectId={projectId}
          data={data}
          onSuccess={onSuccess}
          onClose={() => setCreateOpen(false)}
        />
      )}
    </Box>
  );
};
