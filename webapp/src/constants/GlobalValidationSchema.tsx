import { DefaultParamType, T, TFnType, TranslationKey } from '@tolgee/react';
import { container } from 'tsyringe';
import * as Yup from 'yup';

import { components } from 'tg.service/apiSchema.generated';
import { OrganizationService } from '../service/OrganizationService';
import { SignUpService } from '../service/SignUpService';

type TFunType = TFnType<DefaultParamType, string, TranslationKey>;

type AccountType =
  components['schemas']['PrivateUserAccountModel']['accountType'];

Yup.setLocale({
  // use constant translation keys for messages without values
  mixed: {
    default: 'field_invalid',
    // eslint-disable-next-line react/display-name
    required: () => {
      return <T keyName="Validation - required field" />;
    },
  },
  string: {
    // eslint-disable-next-line react/display-name
    email: () => <T keyName="validation_email_is_not_valid" />,
    // eslint-disable-next-line react/display-name
    min: ({ min }) => (
      <T
        keyName="Field should have at least n chars"
        params={{ min: min.toString() }}
      />
    ),
    // eslint-disable-next-line react/display-name
    max: ({ max }) => (
      <T
        keyName="Field should have maximum of n chars"
        params={{ max: max.toString() }}
      />
    ),
  },
});

export class Validation {
  static readonly USER_PASSWORD = Yup.string().min(8).max(50).required();

  static readonly USER_PASSWORD_WITH_REPEAT_NAKED = {
    password: Validation.USER_PASSWORD,
    passwordRepeat: Yup.string()
      .oneOf([Yup.ref('password'), null], 'Passwords must match')
      .required(),
  };

  static readonly USER_PASSWORD_WITH_REPEAT = Yup.object().shape(
    Validation.USER_PASSWORD_WITH_REPEAT_NAKED
  );

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
    }, container.resolve(SignUpService).validateEmail);

  static readonly SIGN_UP = (t: TFunType, orgRequired: boolean) =>
    Yup.object().shape({
      ...Validation.USER_PASSWORD_WITH_REPEAT_NAKED,
      name: Yup.string().required(),
      email: Yup.string().email().required().test(
        'checkEmailUnique',
        // @tolgee-key validation_email_not_unique
        t('validation_email_not_unique'),
        Validation.createEmailValidation()
      ),
      organizationName: orgRequired
        ? Yup.string().min(3).max(50).required()
        : Yup.string(),
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
        accountType === 'LDAP' ? Yup.string() : Yup.string().email().required(),
    });

  static readonly USER_PASSWORD_CHANGE = Yup.object().shape({
    currentPassword: Yup.string().max(50).required(),
    password: Validation.USER_PASSWORD,
    passwordRepeat: Yup.string()
      .notRequired()
      .oneOf([Yup.ref('password'), null], 'Passwords must match'),
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
  static readonly LANGUAGE_TAG = (t: TFunType) =>
    Yup.string()
      .required()
      .max(20)
      .matches(/^[^,]*$/, {
        // @tolgee-key validation_cannot_contain_coma
        message: t('validation_cannot_contain_coma'),
      });
  static readonly LANGUAGE_ORIGINAL_NAME = Yup.string().required().max(100);
  static readonly LANGUAGE_FLAG_EMOJI = Yup.string().required().max(20);

  static readonly LANGUAGE = (t: TFunType) =>
    Yup.object().shape({
      name: Validation.LANGUAGE_NAME,
      originalName: Validation.LANGUAGE_ORIGINAL_NAME,
      tag: Validation.LANGUAGE_TAG(t),
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
        // @tolgee-key project_creation_add_at_least_one_language
        .min(1, t('project_creation_add_at_least_one_language'))
        .of(Validation.LANGUAGE(t).nullable())
        .test(
          'language-repeated',
          // @tolgee-key create_project_validation_language_repeated
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

  static readonly ORGANIZATION_CREATE_OR_EDIT = (
    t: TFunType,
    slugInitialValue?: string
  ) => {
    const slugSyncValidation = Yup.string()
      .required()
      .min(3)
      .max(60)
      .matches(/^[a-z0-9-]*[a-z]+[a-z0-9-]*$/, {
        message: (
          <T keyName="slug_validation_can_contain_just_lowercase_numbers_hyphens" />
        ),
      });

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
        (v) => container.resolve(OrganizationService).validateSlug(v)
      )(v);
    };
    return Yup.object().shape({
      name: Yup.string().required().min(3).max(50),
      slug: slugSyncValidation.test(
        'slugUnique',
        // @tolgee-key validation_slug_not_unique
        t('validation_slug_not_unique'),
        slugUniqueDebouncedAsyncValidation
      ),
      description: Yup.string().nullable(),
    });
  };

  static readonly INVITE_DIALOG_PROJECT = (t: TFunType) =>
    Yup.object({
      permission: Yup.string(),
      permissionLanguages: Yup.array(Yup.string()),
      type: Yup.string(),
      text: Yup.string().when('type', (val: string) =>
        val === 'email'
          ? Yup.string()
              // @tolgee-key validation_email_is_not_valid
              .email(t('validation_email_is_not_valid'))
              // @tolgee-key Validation - required field
              .required(t('Validation - required field'))
          : // @tolgee-key Validation - required field
            Yup.string().required(t('Validation - required field'))
      ),
    });

  static readonly INVITE_DIALOG_ORGANIZATION = (t: TFunType) =>
    Yup.object({
      permission: Yup.string(),
      type: Yup.string(),
      text: Yup.string().when('type', (val: string) =>
        val === 'email'
          ? Yup.string()
              // @tolgee-key validation_email_is_not_valid
              .email(t('validation_email_is_not_valid'))
              // @tolgee-key Validation - required field
              .required(t('Validation - required field'))
          : // @tolgee-key Validation - required field
            Yup.string().required(t('Validation - required field'))
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
    stripeProductId: Yup.string().required(),
    forOrganizationIds: Yup.array().when('public', {
      is: false,
      then: Yup.array().min(1),
    }),
    prices: Yup.object().when('type', {
      is: 'PAY_AS_YOU_GO',
      then: Yup.object({
        perThousandMtCredits: Yup.number().moreThan(0),
        perThousandTranslations: Yup.number().moreThan(0),
      }),
    }),
  });

  static readonly EE_PLAN_FORM = Yup.object({
    name: Yup.string().required(),
    stripeProductId: Yup.string().required(),
    forOrganizationIds: Yup.array().when('public', {
      is: false,
      then: Yup.array().min(1),
    }),
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
