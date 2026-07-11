import { useRef, useState } from 'react';
import {
  Button,
  ButtonGroup,
  Menu,
  MenuItem,
  styled,
  Tooltip,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';

import LoadingButton from 'tg.component/common/form/LoadingButton';
import { ArrowDropDown, Stars } from 'tg.component/CustomIcons';
import { BatchOperationDialog } from 'tg.views/projects/translations/BatchOperations/OperationsSummary/BatchOperationDialog';
import { BatchJobModel } from 'tg.views/projects/translations/BatchOperations/types';
import { useTranslationsSelector } from 'tg.views/projects/translations/context/TranslationsContext';

import { PreviewBatchDialog } from './PreviewBatchDialog';
import { BasicPromptOption } from './TabBasic';
import { PreviewDatasetDialog } from './PreviewDatasetDialog';

const StyledArrowButton = styled(Button)`
  padding-left: 6px;
  padding-right: 6px;
  min-width: unset !important;
`;

type Props = {
  languageId: number | undefined;
  projectId: number;
  templateValue: string | undefined;
  options: BasicPromptOption[] | undefined;
  providerName: string;
  onBatchFinished: () => void;
  onTestPrompt: () => void;
  loading?: boolean;
  disabled?: boolean;
};

export const PromptPreviewMenu = ({
  languageId,
  projectId,
  templateValue,
  options,
  providerName,
  onBatchFinished,
  onTestPrompt,
  loading,
  disabled,
}: Props) => {
  const [open, setOpen] = useState(false);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const { t } = useTranslate();
  const translationsTotal = useTranslationsSelector(
    (c) => c.translationsTotal ?? 0
  );

  const oneDisabled = languageId === undefined;

  const [batchDialogOpen, setBatchDialogOpen] = useState(false);
  const [datasetDialogOpen, setDatasetDialogOpen] = useState(false);

  const [runningOperation, setRunningOperation] = useState<BatchJobModel>();

  return (
    <>
      <ButtonGroup variant="contained" color="secondary" size="small">
        <Tooltip
          enterDelay={1000}
          title={
            oneDisabled
              ? t('ai_prompt_preview_disabled_hint')
              : t('ai_prompt_preview_hint')
          }
          disableInteractive
        >
          <span>
            <LoadingButton
              disabled={disabled || oneDisabled}
              loading={loading}
              startIcon={<Stars height={18} />}
              ref={buttonRef}
              onClick={onTestPrompt}
              data-cy="ai-prompt-preview-button"
            >
              {t('ai_prompt_preview_label')}
            </LoadingButton>
          </span>
        </Tooltip>
        <StyledArrowButton
          disabled={disabled}
          onClick={() => setOpen(true)}
          ref={buttonRef as any}
          data-cy="ai-prompt-preview-more-button"
        >
          <ArrowDropDown />
        </StyledArrowButton>
      </ButtonGroup>

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
              setDatasetDialogOpen(true);
            }}
            data-cy="ai-prompt-preview-on-dataset"
          >
            {t('ai_prompt_preview_on_dataset')}
          </MenuItem>
          <MenuItem
            onClick={() => {
              setOpen(false);
              setBatchDialogOpen(true);
            }}
            data-cy="ai-prompt-preview-on-all"
          >
            {t('ai_prompt_preview_on_all', { value: translationsTotal })}
          </MenuItem>
        </Menu>
      )}

      {batchDialogOpen && (
        <PreviewBatchDialog
          projectId={projectId}
          numberOfKeys={translationsTotal}
          onClose={() => setBatchDialogOpen(false)}
          providerName={providerName}
          template={templateValue}
          options={options}
          onStart={(data) => setRunningOperation(data)}
        />
      )}

      {runningOperation && (
        <BatchOperationDialog
          operation={runningOperation}
          onClose={() => setRunningOperation(undefined)}
          onFinished={() => {
            setRunningOperation(undefined);
            onBatchFinished();
          }}
        />
      )}

      {datasetDialogOpen && (
        <PreviewDatasetDialog
          onClose={() => setDatasetDialogOpen(false)}
          projectId={projectId}
          providerName={providerName}
          template={templateValue}
          options={options}
          onStart={(data) => setRunningOperation(data)}
        />
      )}
    </>
  );
};
