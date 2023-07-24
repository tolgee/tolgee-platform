export class Link {
  _template: string;

  /**
   * Constructor is private to avoid creating of unrefactorable links
   * @param template
   */
  private constructor(template: string) {
    this._template = template;
  }

  get template(): string {
    return this._template;
  }

  /**
   * creates root in link
   * @param itemTemplate e.g. ":userId" or "users"
   */
  static ofRoot(itemTemplate: string) {
    return this.ofParent(null, itemTemplate);
  }

  /**
   * adds to parent link and returns new link
   * @param link
   * @param itemTemplate
   */
  static ofParent(link: Link | null, itemTemplate: string): Link {
    return new Link(`${link ? link.template : ''}/${itemTemplate}`);
  }

  public build(params?: { [key: string]: string | number }): string {
    let link = this.template;
    params = params ? params : {};
    for (const param of Object.keys(params)) {
      link = link.replace(`:${param}`, params[param].toString());
    }
    return link;
  }

  buildWithOrigin(params?: { [key: string]: string | number }) {
    return window.origin + this.build(params);
  }
}

const p = (param: string) => {
  return `:${param}`;
};

export enum PARAMS {
  INVITATION_CODE = 'invitation_code',
  ENCODED_EMAIL_AND_CODE = 'email_and_code',
  SERVICE_TYPE = 'serviceType',
  PROJECT_ID = 'projectId',
  LANGUAGE_ID = 'languageId',
  API_KEY_ID = 'languageId',
  PAT_ID = 'patId',
  USER_ID = 'userID',
  VERIFICATION_CODE = 'verificationCode',
  ORGANIZATION_SLUG = 'slug',
  TRANSLATION_ID = 'translationId',
  PLAN_ID = 'planId',
}

export class LINKS {
  /**
   * Authentication
   */
  static ROOT = Link.ofRoot('');

  static LOGIN = Link.ofRoot('login');

  static OAUTH_RESPONSE = Link.ofParent(
    LINKS.LOGIN,
    'auth_callback/' + p(PARAMS.SERVICE_TYPE)
  );

  static EMAIL_VERIFICATION = Link.ofParent(
    LINKS.LOGIN,
    'verify_email/' + p(PARAMS.USER_ID) + '/' + p(PARAMS.VERIFICATION_CODE)
  );

  static RESET_PASSWORD_REQUEST = Link.ofRoot('reset_password_request');

  static RESET_PASSWORD = Link.ofRoot('reset_password');

  static RESET_PASSWORD_WITH_PARAMS = Link.ofParent(
    LINKS.RESET_PASSWORD,
    p(PARAMS.ENCODED_EMAIL_AND_CODE)
  );

  static SIGN_UP = Link.ofRoot('sign_up');

  static ACCEPT_INVITATION = Link.ofRoot(
    'accept_invitation/' + p(PARAMS.INVITATION_CODE)
  );

  static GO_TO_CLOUD_BILLING = Link.ofRoot('billing');
  static GO_TO_SELF_HOSTED_BILLING = Link.ofRoot('billing-self-hosted');

  /**
   * Authenticated user stuff
   */

  static USER_SETTINGS = Link.ofRoot('account');

  static USER_API_KEYS = Link.ofParent(LINKS.USER_SETTINGS, 'apiKeys');

  static USER_API_KEYS_GENERATE = Link.ofParent(
    LINKS.USER_API_KEYS,
    'generate'
  );

  static USER_API_KEYS_EDIT = Link.ofParent(
    LINKS.USER_API_KEYS,
    'edit/' + p(PARAMS.API_KEY_ID)
  );

  static USER_API_KEYS_REGENERATE = Link.ofParent(
    LINKS.USER_API_KEYS,
    `regenerate/${p(PARAMS.API_KEY_ID)}`
  );

  static USER_PATS = Link.ofParent(
    LINKS.USER_SETTINGS,
    'personal-access-tokens'
  );

  static USER_PATS_GENERATE = Link.ofParent(LINKS.USER_PATS, 'generate');

  static USER_PATS_REGENERATE = Link.ofParent(
    LINKS.USER_PATS,
    `regenerate/${p(PARAMS.PAT_ID)}`
  );

  static USER_PATS_EDIT = Link.ofParent(
    LINKS.USER_PATS,
    `edit/${p(PARAMS.PAT_ID)}`
  );

  static USER_PROFILE = Link.ofParent(LINKS.USER_SETTINGS, 'profile');

  static USER_ACCOUNT_SECURITY = Link.ofParent(LINKS.USER_SETTINGS, 'security');

  static USER_ACCOUNT_SECURITY_MFA_ENABLE = Link.ofParent(
    LINKS.USER_ACCOUNT_SECURITY,
    'enable-mfa'
  );

  static USER_ACCOUNT_SECURITY_MFA_RECOVERY = Link.ofParent(
    LINKS.USER_ACCOUNT_SECURITY,
    'mfa-recovery-codes'
  );

  static USER_ACCOUNT_SECURITY_MFA_DISABLE = Link.ofParent(
    LINKS.USER_ACCOUNT_SECURITY,
    'disable-mfa'
  );

  /**
   * Administration
   */

  static ADMINISTRATION = Link.ofRoot('administration');

  static ADMINISTRATION_ORGANIZATIONS = Link.ofParent(
    LINKS.ADMINISTRATION,
    'organizations'
  );

  static ADMINISTRATION_USERS = Link.ofParent(LINKS.ADMINISTRATION, 'users');

  static ADMINISTRATION_EE_LICENSE = Link.ofParent(
    LINKS.ADMINISTRATION,
    'ee-license'
  );

  static ADMINISTRATION_BILLING_CLOUD_PLANS = Link.ofParent(
    LINKS.ADMINISTRATION,
    'cloud-plans'
  );

  static ADMINISTRATION_BILLING_CLOUD_PLAN_EDIT = Link.ofParent(
    LINKS.ADMINISTRATION_BILLING_CLOUD_PLANS,
    p(PARAMS.PLAN_ID)
  );

  static ADMINISTRATION_BILLING_CLOUD_PLAN_CREATE = Link.ofParent(
    LINKS.ADMINISTRATION_BILLING_CLOUD_PLANS,
    'create'
  );

  static ADMINISTRATION_BILLING_EE_PLANS = Link.ofParent(
    LINKS.ADMINISTRATION,
    'ee-plans'
  );

  static ADMINISTRATION_BILLING_EE_PLAN_EDIT = Link.ofParent(
    LINKS.ADMINISTRATION_BILLING_EE_PLANS,
    p(PARAMS.PLAN_ID)
  );

  static ADMINISTRATION_BILLING_EE_PLAN_CREATE = Link.ofParent(
    LINKS.ADMINISTRATION_BILLING_EE_PLANS,
    'create'
  );

  /**
   * Organizations
   */
  static ORGANIZATIONS = Link.ofRoot('organizations');

  static ORGANIZATIONS_ADD = Link.ofParent(LINKS.ORGANIZATIONS, 'add');

  static ORGANIZATION = Link.ofParent(
    LINKS.ORGANIZATIONS,
    p(PARAMS.ORGANIZATION_SLUG)
  );

  static ORGANIZATION_PROFILE = Link.ofParent(LINKS.ORGANIZATION, 'profile');

  static ORGANIZATION_MEMBERS = Link.ofParent(LINKS.ORGANIZATION, 'members');

  static ORGANIZATION_MEMBER_PRIVILEGES = Link.ofParent(
    LINKS.ORGANIZATION,
    'member-privileges'
  );

  static ORGANIZATION_INVITATIONS = Link.ofParent(
    LINKS.ORGANIZATION,
    'invitations'
  );

  static ORGANIZATION_BILLING = Link.ofParent(LINKS.ORGANIZATION, 'billing');

  static ORGANIZATION_SUBSCRIPTIONS = Link.ofParent(
    LINKS.ORGANIZATION,
    'subscriptions'
  );

  static ORGANIZATION_INVOICES = Link.ofParent(LINKS.ORGANIZATION, 'invoices');

  static ORGANIZATION_BILLING_TEST_CLOCK_HELPER = Link.ofParent(
    LINKS.ORGANIZATION,
    'billing-test-clock-helper'
  );

  static ORGANIZATION_BILLING_PLANS_EDIT = Link.ofParent(
    LINKS.ORGANIZATION,
    'billing-plans-edit'
  );

  static ORGANIZATION_SUBSCRIPTIONS_SELF_HOSTED_EE = Link.ofParent(
    LINKS.ORGANIZATION_SUBSCRIPTIONS,
    'self-hosted-ee'
  );

  /**
   * Project stuff
   */

  static PROJECTS = Link.ofRoot('projects');

  /**
   * Visible with view permissions
   */

  static AFTER_LOGIN = LINKS.PROJECTS;

  static PROJECT = Link.ofParent(LINKS.PROJECTS, p(PARAMS.PROJECT_ID));

  static PROJECT_ADD = Link.ofParent(LINKS.PROJECTS, 'add');

  static PROJECT_TRANSLATIONS = Link.ofParent(LINKS.PROJECT, 'translations');

  static PROJECT_TRANSLATIONS_SINGLE = Link.ofParent(
    LINKS.PROJECT_TRANSLATIONS,
    'single'
  );

  static PROJECT_EXPORT = Link.ofParent(LINKS.PROJECT, 'export');

  static PROJECT_WEBSOCKETS_PREVIEW = Link.ofParent(
    LINKS.PROJECT,
    'websockets'
  );

  static ACTIVITY_PREVIEW = Link.ofParent(LINKS.PROJECT, 'activity');

  static PROJECT_DASHBOARD = LINKS.PROJECT;

  static PROJECT_INTEGRATE = Link.ofParent(LINKS.PROJECT, 'integrate');

  /**
   * Visible with edit permissions
   */

  static PROJECT_TRANSLATIONS_ADD = Link.ofParent(
    LINKS.PROJECT_TRANSLATIONS,
    'add'
  );

  /**
   * Visible with manage permissions
   */

  static PROJECT_MANAGE = Link.ofParent(LINKS.PROJECT, 'manage');

  static PROJECT_EDIT = Link.ofParent(LINKS.PROJECT_MANAGE, 'edit');

  static PROJECT_LANGUAGES = Link.ofParent(LINKS.PROJECT, 'languages');

  static PROJECT_EDIT_LANGUAGE = Link.ofParent(
    LINKS.PROJECT_LANGUAGES,
    'language/' + p(PARAMS.LANGUAGE_ID)
  );

  static PROJECT_PERMISSIONS = Link.ofParent(
    LINKS.PROJECT_MANAGE,
    'permissions'
  );

  static PROJECT_IMPORT = Link.ofParent(LINKS.PROJECT, 'import');
}
