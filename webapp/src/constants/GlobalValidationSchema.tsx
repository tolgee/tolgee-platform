import { DefaultParamType, T, TFnType, TranslationKey } from '@tolgee/react';
import * as Yup from 'yup';

import { components } from 'tg.service/apiSchema.generated';
import { organizationService } from '../service/OrganizationService';
import { signUpService } from '../service/SignUpService';
import { checkParamNameIsValid } from '@tginternal/editor';
import { validateObject } from 'tg.fixtures/validateObject';

type TranslateFunction = TFnType<DefaultParamType, string, TranslationKey>;

type AccountType =
  components['schemas']['PrivateUserAccountModel']['accountType'];

Yup.setLocale({
  // use constant translation keys for messages without values
  mixed: {
    default: 'field_invalid',
    required: () => {
      return <T keyName="Validation - required field" />;
    },
  },
  string: {
    email: () => <T keyName="validation_email_is_not_valid" />,
    min: ({ min }) => (
      <T
        keyName="Field should have at least n chars"
        params={{ min: min.toString() }}
      />
    ),
    max: ({ max }) => (
      <T
        keyName="Field should have maximum of n chars"
        params={{ max: max.toString() }}
      />
    ),
  },
});

export class Validation {
  static readonly USER_PASSWORD = (t: TranslateFunction) =>
    Yup.string().min(8).max(50).required();

  static readonly RESET_PASSWORD_REQUEST = Yup.object().shape({
    email: Yup.string().email().required(),
  });

  private static readonly createEmailValidation = (): ((
    v
  ) => Promise<boolean>) =>
    debouncedValidation((v) => {
      try {
        Yup.string().required().email().validateSync(v);
        return true;
      } catch (e) {
        return false;
      }
    }, signUpService.validateEmail);

  static readonly SIGN_UP = (t: TranslateFunction, orgRequired: boolean) =>
    Yup.object().shape({
      password: Validation.USER_PASSWORD(t),
      name: Yup.string().required(),
      email: Yup.string()
        .email()
        .required()
        .test(
          'checkEmailUnique',
          t('validation_email_not_unique'),
          Validation.createEmailValidation()
        ),
      organizationName: orgRequired
        ? Yup.string().min(3).max(50).required()
        : Yup.string(),
      userSource: Yup.string().max(255),
    });

  static readonly USER_SETTINGS = (
    accountType: AccountType,
    currentEmail: string
  ) =>
    Yup.object().shape({
      currentPassword: Yup.string()
        .max(50)
        .when('email', {
          is: (email) => email !== currentEmail,
          then: Yup.string().required('Current password is required'),
        }),
      name: Yup.string().required(),
      email:
        accountType === 'MANAGED'
          ? Yup.string()
          : Yup.string().email().required(),
    });

  static readonly USER_PASSWORD_CHANGE = (t: TranslateFunction) =>
    Yup.object().shape({
      currentPassword: Yup.string().max(50).required(),
      password: Validation.USER_PASSWORD(t),
    });

  static readonly PASSWORD_RESET = (t: TranslateFunction) =>
    Yup.object().shape({
      password: Validation.USER_PASSWORD(t),
    });

  static readonly USER_MFA_ENABLE = Yup.object().shape({
    password: Yup.string().max(50).required(),
    otp: Yup.string().required().min(6).max(6),
  });

  static readonly USER_MFA_VIEW_RECOVERY = Yup.object().shape({
    password: Yup.string().max(50).required(),
  });

  static readonly USER_MFA_DISABLE = Yup.object().shape({
    password: Yup.string().max(50).required(),
  });

  static readonly API_KEY_SCOPES = Yup.mixed().test(
    'is-set',
    'Set at least one scope',
    (v) => !!(v as Set<string>).size
  );

  static readonly EDIT_API_KEY = Yup.object().shape({
    scopes: Validation.API_KEY_SCOPES,
    description: Yup.string().required().min(1).max(250),
  });

  static readonly REGENERATE_API_KEY = Yup.object().shape({
    expiresAt: Yup.number().min(new Date().getTime()).nullable(true),
  });

  static readonly CREATE_API_KEY = Yup.object().shape({
    projectId: Yup.number().required(),
    scopes: Yup.mixed().test(
      'is-set',
      'Set at least one scope',
      (v) => !!(v as Set<string>).size
    ),
    description: Yup.string().required().min(1).max(250),
    expiresAt: Yup.number().min(new Date().getTime()).nullable(true),
  });

  static readonly CREATE_PAT = Yup.object().shape({
    description: Yup.string().required().min(1).max(250),
    expiresAt: Yup.number().min(new Date().getTime()).nullable(true),
  });

  static readonly TRANSLATION_KEY = Yup.string().required();

  static readonly TRANSLATION_TRANSLATION = Yup.string();

  static readonly LANGUAGE_NAME = Yup.string().required().max(100);
  static readonly LANGUAGE_TAG = (
    t: TranslateFunction,
    existingTags?: string[]
  ) =>
    Yup.string()
      .required()
      .max(20)
      .test({
        name: 'language-tag-exists',
        test: (value) => !existingTags?.includes(value!),
        message: t('validation_language_tag_exists'),
      })
      .matches(/^[^,]*$/, {
        message: t('validation_cannot_contain_coma'),
      });
  static readonly LANGUAGE_ORIGINAL_NAME = Yup.string().required().max(100);
  static readonly LANGUAGE_FLAG_EMOJI = Yup.string().required().max(20);

  static readonly LANGUAGE = (t: TranslateFunction, existingTags?: string[]) =>
    Yup.object().shape({
      name: Validation.LANGUAGE_NAME,
      originalName: Validation.LANGUAGE_ORIGINAL_NAME,
      tag: Validation.LANGUAGE_TAG(t, existingTags),
      flagEmoji: Validation.LANGUAGE_FLAG_EMOJI,
    });

  static readonly KEY_TRANSLATION_CREATION = (langs: string[]) => {
    const translationValidation = langs.reduce(
      (validation, lang) => ({
        ...validation,
        ['translations.' + lang]: Validation.TRANSLATION_TRANSLATION,
      }),
      {}
    );
    return Yup.object().shape({
      key: Validation.TRANSLATION_KEY,
      ...translationValidation,
    });
  };

  static readonly PROJECT_CREATION = (t: (string) => string) =>
    Yup.object().shape({
      name: Yup.string().required().min(3).max(50),
      languages: Yup.array()
        .required()
        .min(1, t('project_creation_add_at_least_one_language'))
        .of(Validation.LANGUAGE(t, []).nullable())
        .test(
          'language-repeated',
          t('create_project_validation_language_repeated'),
          (values) =>
            new Set(values?.map((i) => i?.name)).size ==
              (values?.length || 0) &&
            new Set(values?.map((i) => i?.tag)).size == (values?.length || 0)
        ),
      baseLanguageTag: Yup.string().required(),
    });

  static readonly PROJECT_SETTINGS = Yup.object().shape({
    name: Yup.string().required().min(3).max(100),
    description: Yup.string().nullable().min(3).max(2000),
  });

  private static slugValidation(min: number, max: number) {
    return Yup.string()
      .min(min)
      .max(max)
      .matches(/^[a-z0-9-]*[a-z]+[a-z0-9-]*$/, {
        message: (
          <T keyName="slug_validation_can_contain_just_lowercase_numbers_hyphens" />
        ),
      });
  }

  static readonly ORGANIZATION_CREATE_OR_EDIT = (
    t: TranslateFunction,
    slugInitialValue?: string
  ) => {
    const slugSyncValidation = Validation.slugValidation(3, 60).required();

    const slugUniqueDebouncedAsyncValidation = (v) => {
      if (slugInitialValue === v) {
        return true;
      }
      return debouncedValidation(
        (v) => {
          try {
            slugSyncValidation.validateSync(v);
            return true;
          } catch (e) {
            return false;
          }
        },
        (v) => organizationService.validateSlug(v)
      )(v);
    };
    return Yup.object().shape({
      name: Yup.string().required().min(3).max(50),
      slug: slugSyncValidation.test(
        'slugUnique',
        t('validation_slug_not_unique'),
        slugUniqueDebouncedAsyncValidation
      ),
      description: Yup.string().nullable(),
    });
  };

  static readonly INVITE_DIALOG_PROJECT = (t: TranslateFunction) =>
    Yup.object({
      permission: Yup.string(),
      permissionLanguages: Yup.array(Yup.string()),
      type: Yup.string(),
      text: Yup.string().when('type', (val: string) =>
        val === 'email'
          ? Yup.string()
              .email(t('validation_email_is_not_valid'))
              .required(t('Validation - required field'))
          : val === 'link'
          ? Yup.string().required(t('Validation - required field'))
          : Yup.string()
      ),
      agency: Yup.number().when('type', (val: string) =>
        val === 'agency' ? Yup.string().required() : Yup.string()
      ),
    });

  static readonly INVITE_DIALOG_ORGANIZATION = (t: TranslateFunction) =>
    Yup.object({
      permission: Yup.string(),
      type: Yup.string(),
      text: Yup.string().when('type', (val: string) =>
        val === 'email'
          ? Yup.string()
              .email(t('validation_email_is_not_valid'))
              .required(t('Validation - required field'))
          : Yup.string().required(t('Validation - required field'))
      ),
    });

  static readonly BILLING_RECIPIENT_EMAIL = Yup.object({
    emailRecipient: Yup.string().required().email(),
  });

  static readonly NAMESPACE_FORM = Yup.object({
    namespace: Yup.string().required().max(100),
  });

  static readonly EE_LICENSE_FORM = Yup.object({
    licenseKey: Yup.string().required().max(100),
  });

  static readonly CLOUD_PLAN_FORM = Yup.object({
    name: Yup.string().required(),
    type: Yup.string().required(),
    stripeProductId: Yup.string().when('free', {
      is: false,
      then: Yup.string().required(),
    }),
    prices: Yup.object().when('type', {
      is: 'PAY_AS_YOU_GO',
      then: Yup.object({
        perThousandMtCredits: Yup.number().min(0),
        perThousandTranslations: Yup.number().min(0),
        perSeat: Yup.number().min(0),
        perThousandKeys: Yup.number().min(0),
      }),
    }),
    free: Yup.boolean(),
  });

  static readonly EE_PLAN_FORM = Yup.object({
    name: Yup.string().required(),
    stripeProductId: Yup.string().when('free', {
      is: false,
      then: Yup.string().required(),
    }),
  });

  static readonly STORAGE_FORM_AZURE_CREATE = Yup.object({
    name: Yup.string().required().max(100),
    publicUrlPrefix: Yup.string().required().max(255),
    azureContentStorageConfig: Yup.object({
      connectionString: Yup.string().required().max(255),
      containerName: Yup.string().required().max(255),
    }),
  });

  static readonly STORAGE_FORM_AZURE_UPDATE = Yup.object().shape({
    name: Yup.string().required().max(100),
    publicUrlPrefix: Yup.string().required().max(255),
    azureContentStorageConfig: Yup.object({
      containerName: Yup.string().required().max(255),
    }),
  });

  static readonly STORAGE_FORM_S3_CREATE = Yup.object().shape({
    name: Yup.string().required().max(100),
    publicUrlPrefix: Yup.string().required().max(255),
    s3ContentStorageConfig: Yup.object({
      bucketName: Yup.string().required().max(255),
      accessKey: Yup.string().required().max(255),
      secretKey: Yup.string().required().max(255),
      endpoint: Yup.string().required().max(255),
      signingRegion: Yup.string().required().max(255),
    }),
  });

  static readonly STORAGE_FORM_S3_UPDATE = Yup.object().shape({
    name: Yup.string().required().max(100),
    publicUrlPrefix: Yup.string().required().max(255),
    s3ContentStorageConfig: Yup.object({
      bucketName: Yup.string().required().max(255),
      endpoint: Yup.string().required().max(255),
      signingRegion: Yup.string().required().max(255),
    }),
  });

  static readonly CONTENT_DELIVERY_FORM = Yup.object().shape({
    name: Yup.string().required().max(100),
    languages: Yup.array().min(1),
    states: Yup.array().min(1),
    slug: Yup.string().when('contentStorageId', {
      is(value?: number) {
        return !!value;
      },
      then: Validation.slugValidation(1, 60),
    }),
  });

  static readonly WEBHOOK_FORM = Yup.object().shape({
    url: Yup.string().required().max(255),
  });

  static readonly NEW_KEY_FORM = (t: TranslateFunction) =>
    Yup.object().shape({
      name: Yup.string().required(),
      pluralParameter: Yup.string().when('isPlural', {
        is: true,
        then: Yup.string().test(
          'invalid-plural-parameter',
          t('validation_invalid_plural_parameter'),
          (value) => checkParamNameIsValid(value ?? '')
        ),
      }),
    });

  static readonly KEY_SETTINGS_FORM = (t: TranslateFunction) =>
    Yup.object().shape({
      custom: Yup.string().test(
        'invalid-custom-values',
        t('validation_invalid_custom_values'),
        validateObject
      ),
    });

  static readonly CREATE_TASK_FORM = (t: TranslateFunction) =>
    Yup.object().shape({
      name: Yup.string().min(3).optional(),
      languages: Yup.array(Yup.number()).min(
        1,
        t('validation_no_language_selected')
      ),
    });

  static readonly UPDATE_TASK_FORM = (t: TranslateFunction) =>
    Yup.object().shape({
      name: Yup.string().min(3).optional(),
    });

  private static readonly validateUrlWithPort = (
    value: string | undefined
  ): boolean => {
    if (!value) return false;
    const urlPattern = /^(http|https):\/\/[\w.-]+(:\d+)?(\/[^\s]*)?$/;
    return urlPattern.test(value);
  };

  static readonly SSO_PROVIDER_ENABLED = (t: TranslateFunction) =>
    Yup.object().shape({
      force: Yup.boolean().required(),
      clientId: Yup.string().required().max(255),
      domain: Yup.string().required().max(255),
      clientSecret: Yup.string().required().max(255),
      authorizationUri: Yup.string()
        .required()
        .max(255)
        .test(
          'is-valid-url-with-port',
          t('sso_invalid_url_format'),
          Validation.validateUrlWithPort
        ),
      tokenUri: Yup.string()
        .required()
        .max(255)
        .test(
          'is-valid-url-with-port',
          t('sso_invalid_url_format'),
          Validation.validateUrlWithPort
        ),
    });

  static readonly SSO_PROVIDER_DISABLED = (t: TranslateFunction) =>
    Yup.object().shape({
      force: Yup.boolean().required(),
      clientId: Yup.string().max(255),
      domain: Yup.string().max(255),
      clientSecret: Yup.string().max(255),
      authorizationUri: Yup.string().max(255),
      tokenUri: Yup.string().max(255),
    });

  static readonly TRANSLATION_AGENCY_FORM = () =>
    Yup.object().shape({
      name: Yup.string().min(3).required(),
      email: Yup.string().min(3).required(),
    });

  static readonly GLOSSARY_CREATE_FORM = (t: TranslateFunction) =>
    Yup.object().shape({
      name: Yup.string().min(3).required(),
      baseLanguage: Yup.object()
        .required()
        .shape({
          tag: Yup.string().min(1).required(),
        }),
      assignedProjects: Yup.array().of(
        Yup.object().shape({
          id: Yup.number().required(),
        })
      ),
    });
}

let GLOBAL_VALIDATION_DEBOUNCE_TIMER: any = undefined;

/**
 * @param syncValidationCallback sync validation callback - must return true to async validation be called
 * @param asyncValidationCallback the async validation
 * @return Promise<true> if valid
 */
const debouncedValidation = (
  syncValidationCallback: (v) => boolean,
  asyncValidationCallback: (v) => Promise<boolean>
): ((v) => Promise<boolean>) => {
  let lastValue = undefined as any;
  let lastResult = undefined as any;
  return (v) => {
    clearTimeout(GLOBAL_VALIDATION_DEBOUNCE_TIMER);
    return new Promise((resolve) => {
      GLOBAL_VALIDATION_DEBOUNCE_TIMER = setTimeout(() => {
        if (lastValue == v) {
          resolve(lastResult);
          return;
        }
        lastResult = syncValidationCallback(v) && asyncValidationCallback(v);
        resolve(lastResult);
        lastValue = v;
      }, 500);
    });
  };
};
