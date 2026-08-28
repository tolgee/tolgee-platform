import { FunctionComponent, useState } from 'react';
import { Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { BaseFormView } from 'tg.component/layout/BaseFormView';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { LINKS, PARAMS } from 'tg.constants/links';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import {
  useOrganizationCreationRefusal,
  usePreferredOrganization,
} from 'tg.globalContext/helpers';
import { NoPermissionsView } from 'tg.component/common/NoPermissionsView';
import { messageService } from 'tg.service/MessageService';

import { OrganizationFields } from './components/OrganizationFields';
import { useHistory } from 'react-router-dom';

type OrganizationBody = components['schemas']['OrganizationDto'];

export const OrganizationCreateView: FunctionComponent<
  React.PropsWithChildren<unknown>
> = () => {
  const loadable = useApiMutation({
    url: '/v2/organizations',
    method: 'post',
  });
  const { t } = useTranslate();
  const { updatePreferredOrganization } = usePreferredOrganization();
  const creationRefusal = useOrganizationCreationRefusal();
  const history = useHistory();
  const [createSubmitted, setCreateSubmitted] = useState(false);

  if (creationRefusal) {
    return <NoPermissionsView reason={creationRefusal} />;
  }

  const onSubmit = (values) => {
    loadable.mutate(
      { content: { 'application/json': values } },
      {
        onSuccess: async (organization) => {
          setCreateSubmitted(true);
          messageService.success(<T keyName="organization_created_message" />);
          const switched = await updatePreferredOrganization(organization.id);
          history.push(
            switched
              ? LINKS.PROJECTS.build()
              : LINKS.ORGANIZATION_PROFILE.build({
                  [PARAMS.ORGANIZATION_SLUG]: organization.slug,
                })
          );
        },
      }
    );
  };

  const initialValues: OrganizationBody = {
    name: '',
    slug: '',
    description: '',
  };

  return (
    <DashboardPage>
      <BaseFormView
        windowTitle={t('create_organization_title')}
        maxWidth="narrow"
        title={<T keyName="create_organization_title" />}
        initialValues={initialValues}
        onSubmit={onSubmit}
        saveActionLoadable={loadable}
        submitDisabledReason={
          createSubmitted ? (
            <Typography
              variant="body2"
              data-cy="organization-create-switching-message"
            >
              <T
                keyName="switching_organization_message"
                defaultValue="Switching organization…"
              />
            </Typography>
          ) : undefined
        }
        validationSchema={Validation.ORGANIZATION_CREATE_OR_EDIT(t, '')}
      >
        <OrganizationFields />
      </BaseFormView>
    </DashboardPage>
  );
};
