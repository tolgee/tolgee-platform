import {
  Box,
  Button,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  styled,
} from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Formik } from 'formik';
import { useState } from 'react';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { TextField } from 'tg.component/common/form/fields/TextField';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { FieldLabel } from 'tg.component/FormField';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { useApiQuery } from 'tg.service/http/useQueryApi';

import { OrderProjectItem } from './OrderProjectItem';
import { ProviderType } from './types';

const StyledDescription = styled('div')`
  margin-bottom: 16px;
`;

const StyledDialogContent = styled(DialogContent)`
  width: 900px;
  max-width: 90vw;
  container: main-container / inline-size;
`;

type Props = {
  provider: ProviderType;
  onClose: () => void;
  preselected: number[];
};

export const OrderTranslationDialog = ({
  provider,
  onClose,
  preselected,
}: Props) => {
  const [page, setPage] = useState(0);
  const { preferredOrganization } = usePreferredOrganization();

  const projects = useApiQuery({
    url: '/v2/organizations/{slug}/projects-with-stats',
    method: 'get',
    path: { slug: preferredOrganization?.slug || '' },
    query: {
      page,
      size: 10,
      sort: ['id,desc'],
    },
    options: {
      keepPreviousData: true,
      enabled: Boolean(preferredOrganization?.slug),
    },
  });

  const { t } = useTranslate();
  return (
    <Dialog open={true} onClose={onClose} maxWidth="md">
      {projects.data ? (
        <Formik
          initialValues={{
            selected: preselected,
            note: '',
            contactDetailsConsent: false,
            inviteProvider: false,
          }}
          onSubmit={(values) => {
            console.log(values);
          }}
        >
          {({ values, setFieldValue, handleSubmit }) => {
            function handleToggle(projectId: number) {
              if (values.selected.includes(projectId)) {
                setFieldValue(
                  'selected',
                  values.selected.filter((id) => id !== projectId)
                );
              } else {
                setFieldValue('selected', [...values.selected, projectId]);
              }
            }

            return (
              <>
                <DialogTitle>
                  {t('order_translation_dialog_title', {
                    provider: provider.name,
                  })}
                </DialogTitle>

                <StyledDialogContent>
                  <StyledDescription>
                    {t('order_translation_dialog_subtitle')}
                  </StyledDescription>

                  <FieldLabel>
                    {t('order_translation_dialog_projects_label')}
                  </FieldLabel>
                  <PaginatedHateoasList
                    onPageChange={setPage}
                    loadable={projects}
                    renderItem={(i) => (
                      <OrderProjectItem
                        key={i.id}
                        project={i}
                        selected={values.selected.includes(i.id)}
                        onSelectToggle={() => handleToggle(i.id)}
                      />
                    )}
                  />

                  <Box mt={3}>
                    <FieldLabel>
                      {t('order_translation_dialog_note_label')}
                    </FieldLabel>
                    <TextField
                      minRows={2}
                      size="small"
                      name="note"
                      multiline
                      placeholder={t(
                        'order_translation_dialog_note_placeholder'
                      )}
                      sx={{ mt: 0 }}
                    />
                  </Box>

                  <Box mt={3} display="grid" gap={1}>
                    <FormControlLabel
                      label={t('order_translation_dialog_consent')}
                      control={
                        <Checkbox
                          checked={values.contactDetailsConsent}
                          onChange={() =>
                            setFieldValue(
                              'contactDetailsConsent',
                              !values.contactDetailsConsent
                            )
                          }
                        />
                      }
                    />
                    <FormControlLabel
                      label={t('order_translation_dialog_send_invitation')}
                      control={
                        <Checkbox
                          checked={values.inviteProvider}
                          onChange={() =>
                            setFieldValue(
                              'inviteProvider',
                              !values.inviteProvider
                            )
                          }
                        />
                      }
                    />
                  </Box>
                </StyledDialogContent>

                <DialogActions>
                  <Button onClick={() => onClose()}>
                    {t('order_translation_dialog_cancel')}
                  </Button>
                  <Button
                    variant="contained"
                    color="primary"
                    onClick={() => handleSubmit()}
                  >
                    {t('order_translation_dialog_submit')}
                  </Button>
                </DialogActions>
              </>
            );
          }}
        </Formik>
      ) : (
        <BoxLoading my={10} mx={16} />
      )}
    </Dialog>
  );
};
