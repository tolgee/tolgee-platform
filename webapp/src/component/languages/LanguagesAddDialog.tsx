import { FunctionComponent, useState } from 'react';
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
} from '@mui/material';

import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { ApiError } from 'tg.service/http/ApiError';

import { ResourceErrorComponent } from '../common/form/ResourceErrorComponent';
import { CreateLanguagesField } from './CreateLanguagesField';
import { T } from '@tolgee/react';
import { messageService } from 'tg.service/MessageService';

type LanguageDto = components['schemas']['LanguageRequest'];
type LanguageModel = components['schemas']['LanguageModel'];

type Props = {
  onClose: () => void;
  onCreated?: (added: LanguageModel[]) => void;
  onChangesMade: () => void;
  existingLanguages: string[];
};

export const LanguagesAddDialog: FunctionComponent<Props> = ({
  onClose,
  onCreated,
  onChangesMade,
  existingLanguages,
}) => {
  const project = useProject();
  const { refetchUsage } = useGlobalActions();
  const createLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/languages',
    method: 'post',
    fetchOptions: {
      disableErrorNotification: true,
    },
  });

  const [values, setValues] = useState<LanguageDto[]>([]);
  const [addedValues, setAddedValues] = useState<LanguageModel[]>([]);

  const handleSubmit = async () => {
    const results: LanguageModel[] = addedValues;
    try {
      for (const value of values) {
        results.push(
          await createLoadable.mutateAsync({
            path: {
              projectId: project.id,
            },
            content: {
              'application/json': value,
            },
          })
        );
      }
      onCreated?.(results);
      setValues([]);
      onChangesMade();
      messageService.success(<T keyName="languages_created_message" />);
    } catch (e: any) {
      // remove already added languages
      setValues((values) =>
        values.filter(({ tag }) => !results.find((r) => r.tag === tag))
      );
      // remember which are already added
      setAddedValues(results);
      setServerError(e);
      onChangesMade();
    }
    refetchUsage();
  };

  const [serverError, setServerError] = useState(
    undefined as ApiError | undefined | null
  );

  return (
    <Dialog open={true} onClose={() => onClose()}>
      <DialogTitle>
        <T keyName="languages_add_dialog_title" />
      </DialogTitle>
      <DialogContent
        sx={{
          width: '85vw',
          maxWidth: 600,
        }}
      >
        {serverError && (
          <Box ml={2} mr={2}>
            <ResourceErrorComponent error={serverError} limit={1} />
          </Box>
        )}
        <CreateLanguagesField
          autoFocus
          value={values}
          existingTags={[
            ...existingLanguages,
            ...addedValues.map((l) => l.tag),
            ...values.map((l) => l.tag),
          ]}
          onChange={(value) => {
            setValues(value);
            setServerError(undefined);
          }}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={() => onClose()}>
          <T keyName="languages_add_dialog_cancel" />
        </Button>
        <Button
          color="primary"
          onClick={handleSubmit}
          disabled={values.length === 0}
          data-cy="languages-add-dialog-submit"
        >
          <T keyName="languages_add_dialog_submit" />
        </Button>
      </DialogActions>
    </Dialog>
  );
};
