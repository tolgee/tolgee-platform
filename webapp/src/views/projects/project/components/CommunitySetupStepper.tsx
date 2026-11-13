import { ReactNode, useState } from 'react';
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Step,
  StepButton,
  StepContent,
  StepLabel,
  Stepper,
  Typography,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { FieldLabel } from 'tg.component/FormField';
import { TextField } from 'tg.component/common/TextField';
import { CharacterCounter } from 'tg.component/common/CharacterCounter';
import { COMMUNITY_TEXT_MAX_LENGTH } from 'tg.constants/GlobalValidationSchema';
import { components } from 'tg.service/apiSchema.generated';
import {
  isInContextValueValid,
  inContextValueToSave,
} from 'tg.views/projects/project/components/communityAppUrl';
import { CommunityInContextContent } from 'tg.views/projects/project/components/CommunityInContextContent';
import { CommunityShareContent } from 'tg.views/projects/project/components/CommunityShareContent';
import { CommunityStepIcon } from 'tg.views/projects/project/components/CommunityStepIcon';
import {
  COMMUNITY_SETUP_STEPS,
  CommunitySetupStep,
  isCommunityStepDone,
  useCommunitySetupTexts,
} from 'tg.views/projects/project/components/useCommunitySetupTexts';

const TextStep = (props: {
  label: string;
  placeholder: string;
  value: string;
  onChange: (value: string) => void;
  'data-cy': string;
}) => (
  <Box>
    <FieldLabel>{props.label}</FieldLabel>
    <TextField
      value={props.value}
      onChange={(e) => props.onChange(e.target.value)}
      multiline
      minRows={6}
      fullWidth
      placeholder={props.placeholder}
      minHeight={false}
      inputProps={{ maxLength: COMMUNITY_TEXT_MAX_LENGTH }}
      data-cy={props['data-cy']}
    />
    <CharacterCounter
      currentCount={props.value.length}
      maxLimit={COMMUNITY_TEXT_MAX_LENGTH}
    />
  </Box>
);

const LAST_STEP = COMMUNITY_SETUP_STEPS.length - 1;

type CommunityPageRequest = components['schemas']['CommunityPageRequest'];

type Props = {
  projectId: number;
  values: CommunityPageRequest;
  saving: boolean;
  onSave: (values: CommunityPageRequest, onSaved: () => void) => void;
  onClose: () => void;
};

export const CommunitySetupStepper = ({
  projectId,
  values,
  saving,
  onSave,
  onClose,
}: Props) => {
  const { t } = useTranslate();
  const stepTexts = useCommunitySetupTexts();
  const [active, setActive] = useState(0);
  const [skipped, setSkipped] = useState<number[]>([]);
  const [draft, setDraft] = useState(values);
  const patch = (change: Partial<CommunityPageRequest>) =>
    setDraft({ ...draft, ...change });
  const canLeave = isInContextValueValid(draft) && !saving;

  const saveDraftThen = (onSaved: () => void) => {
    if (canLeave) onSave(inContextValueToSave(draft), onSaved);
  };

  const saveAndClose = () => saveDraftThen(onClose);
  const saveAndOpen = (index: number) => saveDraftThen(() => setActive(index));

  const saveAndGoOn = (nextSkipped: number[]) =>
    saveDraftThen(() => {
      setSkipped(nextSkipped);
      if (active === LAST_STEP) {
        onClose();
        return;
      }
      setActive(active + 1);
    });

  const next = () => saveAndGoOn(skipped.filter((s) => s !== active));
  const skip = () => saveAndGoOn([...skipped, active]);

  const stepContent: Record<CommunitySetupStep, ReactNode> = {
    description: (
      <TextStep
        label={stepTexts.descriptionField.fieldLabel}
        placeholder={stepTexts.descriptionField.placeholder}
        value={draft.communityDescription ?? ''}
        onChange={(communityDescription) => patch({ communityDescription })}
        data-cy="community-setup-description"
      />
    ),
    guidelines: (
      <TextStep
        label={stepTexts.guidelinesField.fieldLabel}
        placeholder={stepTexts.guidelinesField.placeholder}
        value={draft.communityGuidelines ?? ''}
        onChange={(communityGuidelines) => patch({ communityGuidelines })}
        data-cy="community-setup-guidelines"
      />
    ),
    share: (
      <CommunityShareContent
        projectId={projectId}
        shared={draft.communityShared}
        onSharedChange={(communityShared) => patch({ communityShared })}
      />
    ),
    inContext: <CommunityInContextContent value={draft} onChange={patch} />,
  };

  const steps = COMMUNITY_SETUP_STEPS.map((step) => ({
    ...stepTexts[step],
    content: stepContent[step],
    completed: isCommunityStepDone(step, draft),
  }));

  return (
    <Dialog
      open
      onClose={saveAndClose}
      fullWidth
      maxWidth="sm"
      data-cy="community-setup-dialog"
    >
      <DialogTitle>
        {t('community_setup_title', 'Set up your community page')}
      </DialogTitle>
      <DialogContent>
        <Stepper nonLinear orientation="vertical" activeStep={active}>
          {steps.map((step, index) => (
            <Step key={index} completed={step.completed}>
              <StepButton
                onClick={() => saveAndOpen(index)}
                disabled={!canLeave}
                data-cy="community-setup-step"
                data-cy-step={index}
              >
                <StepLabel
                  StepIconComponent={CommunityStepIcon}
                  optional={
                    skipped.includes(index) ? (
                      <Typography variant="caption" color="text.secondary">
                        {t('community_setup_skipped', 'Skipped')}
                      </Typography>
                    ) : undefined
                  }
                >
                  {step.label}
                </StepLabel>
              </StepButton>
              <StepContent>
                <Box display="grid" gap={2}>
                  <Typography variant="body2" color="text.secondary">
                    {step.hint}
                  </Typography>
                  {step.content}
                </Box>
              </StepContent>
            </Step>
          ))}
        </Stepper>
      </DialogContent>
      <DialogActions>
        <Button
          onClick={saveAndClose}
          disabled={!canLeave}
          data-cy="community-setup-later"
        >
          {t('community_setup_finish_later', 'Finish later')}
        </Button>
        <Box flexGrow={1} />
        <Button
          color="primary"
          onClick={skip}
          disabled={!canLeave}
          data-cy="community-setup-skip"
        >
          {t('community_setup_skip', 'Skip')}
        </Button>
        <Button
          color="primary"
          variant="contained"
          onClick={next}
          disabled={!canLeave}
          data-cy="community-setup-next"
        >
          {active === LAST_STEP
            ? t('community_setup_done', 'Done')
            : t('community_setup_next', 'Next')}
        </Button>
      </DialogActions>
    </Dialog>
  );
};
