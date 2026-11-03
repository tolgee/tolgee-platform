import { useState } from 'react';
import * as Yup from 'yup';
import {
  Box,
  Collapse,
  Dialog,
  IconButton,
  DialogContent,
  DialogTitle,
  Typography,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import {
  Check,
  ChevronDown,
  ChevronUp,
  Edit02,
} from '@untitled-ui/icons-react';

import { LinkExternal } from 'tg.component/LinkExternal';
import { FieldLabel } from 'tg.component/FormField';
import {
  LoadableType,
  StandardForm,
} from 'tg.component/common/form/StandardForm';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { CharacterCounter } from 'tg.component/common/CharacterCounter';
import { PROJECT_DESCRIPTION_MAX_LENGTH } from 'tg.constants/GlobalValidationSchema';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { components } from 'tg.service/apiSchema.generated';
import { CommunityInContextDialog } from 'tg.views/projects/project/components/CommunityInContextDialog';
import { CommunitySetupStepper } from 'tg.views/projects/project/components/CommunitySetupStepper';
import { CommunityShareDialog } from 'tg.views/projects/project/components/CommunityShareDialog';
import { StyledStepIndex } from 'tg.views/projects/project/components/CommunityStepIndex';
import {
  CommunitySetupStep,
  useCommunitySetupTexts,
} from 'tg.views/projects/project/components/useCommunitySetupTexts';

const TextDialog = (props: {
  title: string;
  hint: string;
  label: string;
  placeholder: string;
  value: string;
  saveActionLoadable: LoadableType;
  onClose: () => void;
  onSave: (value: string) => void;
  'data-cy': string;
}) => {
  return (
    <Dialog
      open
      onClose={props.onClose}
      fullWidth
      maxWidth="sm"
      data-cy={props['data-cy']}
    >
      <DialogTitle sx={{ pb: 1 }}>{props.title}</DialogTitle>
      <DialogContent>
        <StandardForm
          initialValues={{ text: props.value }}
          validationSchema={Yup.object({
            text: Yup.string().max(PROJECT_DESCRIPTION_MAX_LENGTH),
          })}
          saveActionLoadable={props.saveActionLoadable}
          onSubmit={(values) => props.onSave(values.text)}
          onCancel={props.onClose}
        >
          {({ values }) => (
            <Box mb={2}>
              <Typography variant="body2" color="text.secondary" mb={2}>
                {props.hint}
              </Typography>
              <FieldLabel>{props.label}</FieldLabel>
              <TextField
                name="text"
                size="small"
                multiline
                minRows={6}
                placeholder={props.placeholder}
                minHeight={false}
                data-cy="community-page-dialog-text"
                sx={{ mt: 0 }}
              />
              <CharacterCounter
                currentCount={values.text.length}
                maxLimit={PROJECT_DESCRIPTION_MAX_LENGTH}
              />
            </Box>
          )}
        </StandardForm>
      </DialogContent>
    </Dialog>
  );
};

type CommunityPageRequest = components['schemas']['CommunityPageRequest'];

const TEXT_FIELDS = {
  description: 'communityDescription',
  guidelines: 'communityGuidelines',
} as const;

export const CommunityPageSettings = (props: {
  canEdit: boolean;
  setupOpen: boolean;
  onSetupClose: () => void;
}) => {
  const { t } = useTranslate();
  const project = useProject();
  const [values, setValues] = useState<CommunityPageRequest>(project);
  const [editing, setEditing] = useState<CommunitySetupStep | null>(null);
  const [open, setOpen] = useState(true);

  const communityPageLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/community-page',
    method: 'put',
    invalidatePrefix: '/v2/projects',
  });

  const saveCommunityPage = (
    change: Partial<CommunityPageRequest>,
    onSaved: () => void
  ) => {
    const next = { ...values, ...change };
    const keys = Object.keys(change) as (keyof CommunityPageRequest)[];
    const unchanged = keys.every((key) => next[key] === values[key]);
    if (unchanged) {
      onSaved();
      return;
    }
    communityPageLoadable.mutate(
      {
        path: { projectId: project.id },
        content: {
          'application/json': {
            communityDescription: next.communityDescription,
            communityGuidelines: next.communityGuidelines,
            communityShared: next.communityShared,
            communityInContext: next.communityInContext,
            communityAppUrl: next.communityAppUrl,
          },
        },
      },
      {
        onSuccess(updated) {
          setValues(updated);
          onSaved();
        },
      }
    );
  };

  const closeEditor = () => setEditing(null);
  const closeEditorUnlessSaving = () =>
    !communityPageLoadable.isLoading && closeEditor();
  const openEditor = (step: CommunitySetupStep) => {
    communityPageLoadable.reset();
    setEditing(step);
  };

  const stepTexts = useCommunitySetupTexts();

  const textFields = {
    description: stepTexts.descriptionField,
    guidelines: stepTexts.guidelinesField,
  };

  const rows: { key: CommunitySetupStep; done: boolean }[] = [
    { key: 'description', done: !!values.communityDescription },
    { key: 'guidelines', done: !!values.communityGuidelines },
    { key: 'share', done: values.communityShared },
    { key: 'inContext', done: values.communityInContext },
  ];

  return (
    <Box
      mt={3}
      p={2.5}
      border={1}
      borderColor="divider"
      borderRadius="8px"
      bgcolor="background.paper"
      data-cy="project-settings-community-page"
    >
      <Box
        display="flex"
        justifyContent="space-between"
        alignItems="center"
        gap={2}
      >
        <Box
          component="button"
          type="button"
          onClick={() => setOpen(!open)}
          aria-expanded={open}
          display="flex"
          alignItems="center"
          gap={1}
          p={0}
          border="none"
          bgcolor="transparent"
          color="inherit"
          sx={{ cursor: 'pointer', font: 'inherit' }}
          data-cy="project-settings-community-page-toggle"
        >
          <Typography fontSize={16}>
            {t('community_settings_page_title', 'Community project page')}
          </Typography>
          {open ? (
            <ChevronUp width={20} height={20} />
          ) : (
            <ChevronDown width={20} height={20} />
          )}
        </Box>
        <LinkExternal
          href={LINKS.PROJECT_COMMUNITY.build({
            [PARAMS.PROJECT_ID]: project.id,
          })}
          underline="hover"
          variant="body2"
          data-cy="project-settings-community-page-view"
        >
          {t('community_settings_view_page', 'View page')}
        </LinkExternal>
      </Box>
      <Collapse in={open}>
        <Box
          mt={1}
          display="grid"
          gridTemplateColumns="auto auto minmax(0, 1fr) auto"
          alignItems="center"
          columnGap={1.5}
        >
          {rows.map((row, index) => (
            <Box
              key={row.key}
              display="contents"
              data-cy="project-settings-community-page-item"
              data-cy-item={row.key}
            >
              <StyledStepIndex
                className={row.done ? 'done' : undefined}
                data-cy="project-settings-community-page-item-state"
                data-cy-done={row.done}
              >
                {row.done ? <Check width={16} height={16} /> : index + 1}
              </StyledStepIndex>
              <Typography fontSize={14} fontWeight={500}>
                {stepTexts[row.key].label}
              </Typography>
              <Typography
                fontSize={14}
                color="text.secondary"
                fontStyle="italic"
                noWrap
              >
                {stepTexts[row.key].short}
              </Typography>
              <IconButton
                size="small"
                disabled={!props.canEdit}
                onClick={() => openEditor(row.key)}
                aria-label={t('community_settings_edit', 'Edit')}
                data-cy="project-settings-community-page-edit"
                data-cy-item={row.key}
              >
                <Edit02 width={19} height={19} />
              </IconButton>
            </Box>
          ))}
        </Box>
      </Collapse>
      {(editing === 'description' || editing === 'guidelines') && (
        <TextDialog
          title={stepTexts[editing].label}
          hint={stepTexts[editing].hint}
          label={textFields[editing].fieldLabel}
          placeholder={textFields[editing].placeholder}
          value={values[TEXT_FIELDS[editing]] ?? ''}
          saveActionLoadable={communityPageLoadable}
          onClose={closeEditorUnlessSaving}
          onSave={(value) =>
            saveCommunityPage({ [TEXT_FIELDS[editing]]: value }, closeEditor)
          }
          data-cy="community-page-dialog"
        />
      )}
      {editing === 'inContext' && (
        <CommunityInContextDialog
          value={values}
          saving={communityPageLoadable.isLoading}
          onSave={(value) => saveCommunityPage(value, closeEditor)}
        />
      )}
      {editing === 'share' && (
        <CommunityShareDialog
          projectId={project.id}
          shared={values.communityShared}
          saving={communityPageLoadable.isLoading}
          onSave={(communityShared) =>
            saveCommunityPage({ communityShared }, closeEditor)
          }
        />
      )}
      {props.setupOpen && props.canEdit && (
        <CommunitySetupStepper
          projectId={project.id}
          values={values}
          saving={communityPageLoadable.isLoading}
          onSave={saveCommunityPage}
          onClose={props.onSetupClose}
        />
      )}
    </Box>
  );
};
