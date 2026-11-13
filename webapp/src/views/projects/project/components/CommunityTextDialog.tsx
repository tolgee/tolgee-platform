import * as Yup from 'yup';
import {
  Box,
  Dialog,
  DialogContent,
  DialogTitle,
  Typography,
} from '@mui/material';

import { FieldLabel } from 'tg.component/FormField';
import {
  LoadableType,
  StandardForm,
} from 'tg.component/common/form/StandardForm';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { CharacterCounter } from 'tg.component/common/CharacterCounter';
import { COMMUNITY_TEXT_MAX_LENGTH } from 'tg.constants/GlobalValidationSchema';

export const CommunityTextDialog = (props: {
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
            text: Yup.string().max(COMMUNITY_TEXT_MAX_LENGTH),
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
                maxLimit={COMMUNITY_TEXT_MAX_LENGTH}
              />
            </Box>
          )}
        </StandardForm>
      </DialogContent>
    </Dialog>
  );
};
