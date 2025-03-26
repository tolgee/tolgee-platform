import { Box, Button, Menu, MenuItem, styled } from '@mui/material';
import { useRef, useState } from 'react';
import { CompactListSubheader } from 'tg.component/ListComponents';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';

type PromptModel = components['schemas']['PromptModel'];

const StyledCompactButton = styled(Button)`
  padding: 4px 8px;
  font-size: 13px;
  align-self: center;
  min-height: 0px !important;
  font-style: normal;
  font-weight: 500;
  line-height: normal;
`;

type Props = {
  onSelect: (prompt: PromptModel) => void;
  projectId: number;
};

export const PromptLoadMenu = ({ onSelect, projectId }: Props) => {
  const [open, setOpen] = useState(false);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const existingPrompts = useApiQuery({
    url: '/v2/projects/{projectId}/prompts',
    method: 'get',
    path: {
      projectId,
    },
    query: {
      size: 1000,
    },
  });

  const deletePrompt = useApiMutation({
    url: '/v2/projects/{projectId}/prompts/{promptId}',
    method: 'delete',
    invalidatePrefix: '/v2/projects/{projectId}/prompts',
  });

  return (
    <Box>
      <Button
        variant="outlined"
        size="small"
        ref={buttonRef}
        onClick={() => setOpen(true)}
      >
        Existing prompts...
      </Button>
      {open && (
        <Menu
          open={true}
          onClose={() => setOpen(false)}
          anchorEl={buttonRef.current}
          MenuListProps={{ sx: { minWidth: 250 } }}
        >
          <CompactListSubheader sx={{ pt: 0 }}>
            Load existing prompt
          </CompactListSubheader>
          {existingPrompts.data?._embedded?.prompt?.map((item) => (
            <MenuItem
              key={item.id}
              onClick={() => {
                setOpen(false);
                onSelect(item);
              }}
              sx={{
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center',
              }}
            >
              <Box>{item.name}</Box>
              <StyledCompactButton
                variant="outlined"
                color="error"
                size="small"
                onClick={() => {
                  deletePrompt.mutate({
                    path: { projectId },
                    query: {
                      promptId: item.id,
                    },
                  });
                }}
              >
                Delete
              </StyledCompactButton>
            </MenuItem>
          ))}
          {existingPrompts.data?.page?.totalElements === 0 && (
            <MenuItem disabled>No prompts yet</MenuItem>
          )}
        </Menu>
      )}
    </Box>
  );
};
