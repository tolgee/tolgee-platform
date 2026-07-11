import { useRef, useState } from 'react';
import { Box, IconButton, Menu, MenuItem, Tooltip } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { DotsVertical } from '@untitled-ui/icons-react';

import { CompactListSubheader } from 'tg.component/ListComponents';
import { components } from 'tg.service/apiSchema.generated';
import { useApiQuery } from 'tg.service/http/useQueryApi';

type PromptDto = components['schemas']['PromptDto'];

export type PromptItem = PromptDto & { id?: number };

type Props = {
  onSelect: (prompt: PromptItem) => void;
  projectId: number;
};

export const PromptLoadMenu = ({ onSelect, projectId }: Props) => {
  const [open, setOpen] = useState(false);
  const { t } = useTranslate();
  const buttonRef = useRef<HTMLButtonElement>(null);
  const existingPrompts = useApiQuery({
    url: '/v2/projects/{projectId}/prompts',
    method: 'get',
    path: {
      projectId,
    },
    query: {
      size: 1000,
      sort: ['name'],
    },
  });

  const defaultPrompt = useApiQuery({
    url: '/v2/projects/{projectId}/prompts/default',
    method: 'get',
    path: {
      projectId,
    },
  });

  const prompts: (PromptDto & { id?: number })[] = [];

  if (defaultPrompt.data) {
    prompts.push(defaultPrompt.data);
  }

  existingPrompts.data?._embedded?.prompts?.forEach((item) => {
    prompts.push(item);
  });

  return (
    <Box>
      <Tooltip title={t('ai_prompt_open_existing_prompt')} disableInteractive>
        <IconButton
          ref={buttonRef}
          onClick={() => setOpen(true)}
          data-cy="ai-prompt-open-existing-prompt-select"
        >
          <DotsVertical height={20} width={20} />
        </IconButton>
      </Tooltip>
      {open && (
        <Menu
          open={true}
          onClose={() => setOpen(false)}
          anchorEl={buttonRef.current}
          MenuListProps={{ sx: { minWidth: 250 } }}
        >
          <CompactListSubheader sx={{ pt: 0 }}>
            {t('ai_prompt_open_existing_prompt')}
          </CompactListSubheader>
          {prompts?.map((item) => (
            <MenuItem
              data-cy="ai-prompt-open-existing-prompt-item"
              key={item.id ?? -1}
              onClick={() => {
                setOpen(false);
                onSelect(item);
              }}
            >
              {item.id !== undefined ? item.name : t('ai_prompt_default_name')}
            </MenuItem>
          ))}
        </Menu>
      )}
    </Box>
  );
};
