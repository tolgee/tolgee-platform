import { useTranslate } from '@tolgee/react';
import { Box, Button, IconButton, styled } from '@mui/material';
import { Plus, Edit02 } from '@untitled-ui/icons-react';
import { useState } from 'react';
import { AiProjectDescriptionDialog } from './AiProjectDescriptionDialog';
import { Stars } from 'tg.component/CustomIcons';
import clsx from 'clsx';

const EXAMPLE = 'App for teaching children about the world.';

const StyledWrapper = styled(Box)`
  border-radius: 4px;
  border: 1px solid ${({ theme }) => theme.palette.exampleBanner.border};
  padding: 16px 20px;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  &.empty {
    background: ${({ theme }) => theme.palette.exampleBanner.background};
  }
`;

const StyledLabel = styled(Box)`
  display: flex;
  gap: 8px;
  color: ${({ theme }) => theme.palette.primary.main};
`;

const StyledItems = styled(Box)`
  display: grid;
  color: ${({ theme }) => theme.palette.exampleBanner.text};
`;

type Props = {
  description: string | undefined;
};

export const AiProjectDescription = ({ description }: Props) => {
  const { t } = useTranslate();

  const [dialogOpen, setDialogOpen] = useState(false);

  return (
    <StyledWrapper
      className={clsx({ empty: !description })}
      data-cy="ai-customization-project-description"
    >
      {description ? (
        <>
          <Box py="8px">{description}</Box>
          <Box>
            <IconButton
              onClick={() => setDialogOpen(true)}
              data-cy="ai-customization-project-description-edit"
            >
              <Edit02 width={19} height={19} />
            </IconButton>
          </Box>
        </>
      ) : (
        <>
          <Box display="flex" gap={1} py="8px">
            <StyledLabel>
              <Stars />
              <Box>{t('project_ai_prompt_example_label')}</Box>
            </StyledLabel>
            <StyledItems>
              <Box>{EXAMPLE}</Box>
            </StyledItems>
          </Box>
          <Box>
            <Button
              color="primary"
              variant="contained"
              startIcon={<Plus width={19} height={19} />}
              onClick={() => setDialogOpen(true)}
              data-cy="ai-customization-project-description-add"
            >
              {t('project_ai_prompt_add')}
            </Button>
          </Box>
        </>
      )}
      {dialogOpen && (
        <AiProjectDescriptionDialog
          placeholder={EXAMPLE}
          onClose={() => setDialogOpen(false)}
          currentValue={description || ''}
        />
      )}
    </StyledWrapper>
  );
};
