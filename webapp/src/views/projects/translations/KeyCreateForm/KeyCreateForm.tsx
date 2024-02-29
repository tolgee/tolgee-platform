import { useEffect } from 'react';
import { T, TFnType, useTranslate } from '@tolgee/react';
import { Formik } from 'formik';

import { components } from 'tg.service/apiSchema.generated';
import { useProject } from 'tg.hooks/useProject';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useUrlSearch } from 'tg.hooks/useUrlSearch';
import { LINKS } from 'tg.constants/links';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { FormBody } from './FormBody';
import { useGlobalActions } from 'tg.globalContext/GlobalContext';
import { useTranslationsWebsocketBlocker } from '../context/useTranslationsWebsocketBlocker';
import { messageService } from 'tg.service/MessageService';
import { redirectionActions } from 'tg.store/global/RedirectionActions';
import { TolgeeFormat, tolgeeFormatGenerateIcu } from '@tginternal/editor';
import { Validation } from 'tg.constants/GlobalValidationSchema';

type KeyWithDataModel = components['schemas']['KeyWithDataModel'];

export type ValuesCreateType = {
  name: string;
  baseValue: TolgeeFormat;
  tags: string[];
  description: string | undefined;
  isPlural: boolean;
  pluralParameter: string;
};

type Props = {
  baseLanguage: string;
  onSuccess?: (data: KeyWithDataModel) => void;
  onCancel?: () => void;
  onDirtyChange?: (dirty: boolean) => void;
  autofocus?: boolean;
};

export const KeyCreateForm: React.FC<Props> = ({
  baseLanguage,
  onSuccess,
  onCancel,
  onDirtyChange,
  autofocus,
}) => {
  const { t } = useTranslate();
  const project = useProject();
  const permissions = useProjectPermissions();
  const { refetchUsage } = useGlobalActions();

  const keyName = useUrlSearch().key as string;
  const namespace = useUrlSearch().ns as string;

  const createKey = useApiMutation({
    url: '/v2/projects/{projectId}/keys/create',
    method: 'post',
  });

  useTranslationsWebsocketBlocker(createKey.isLoading);

  const handleSubmit = (values: ValuesCreateType) => {
    const actualParameter = values.isPlural
      ? values.pluralParameter || 'value'
      : undefined;
    return createKey.mutateAsync(
      {
        path: { projectId: project.id },
        content: {
          'application/json': {
            ...values,
            isPlural: Boolean(values.isPlural),
            translations: {
              [baseLanguage]: tolgeeFormatGenerateIcu(
                {
                  ...values.baseValue,
                  parameter: actualParameter,
                },
                !project.icuPlaceholders
              ),
            },
          },
        },
      },
      {
        onSuccess(data) {
          messageService.success(<T keyName="translations_key_created" />);
          onSuccess?.(data);
          refetchUsage();
        },
      }
    );
  };

  const baseValue: TolgeeFormat = {
    variants: { other: '' },
  };

  const canCreateKeys = permissions.satisfiesPermission('keys.create');
  useEffect(() => {
    if (!canCreateKeys) {
      redirectionActions.redirect.dispatch(LINKS.AFTER_LOGIN.build());
      messageService.error(
        <T keyName="translation_single_no_permission_create" />
      );
    }
  }, [canCreateKeys]);

  return canCreateKeys ? (
    <Formik
      initialValues={{
        name: keyName,
        baseValue,
        tags: [],
        description: '',
        namespace,
        isPlural: false,
        pluralParameter: 'value',
      }}
      onSubmit={handleSubmit}
      validationSchema={Validation.NEW_KEY_FORM(t as TFnType)}
    >
      {(formik) => {
        useEffect(() => {
          onDirtyChange?.(formik.dirty);
        }, [formik.dirty]);
        return <FormBody onCancel={onCancel} autofocus={autofocus} />;
      }}
    </Formik>
  ) : null;
};
