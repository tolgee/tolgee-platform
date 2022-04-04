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
  USER_ID = 'userID',
  VERIFICATION_CODE = 'verificationCode',
  ORGANIZATION_SLUG = 'slug',
  TRANSLATION_ID = 'translationId',
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

  /**
   * Authenticated user stuff
   */

  static USER_API_KEYS = Link.ofRoot('apiKeys');

  static USER_API_KEYS_GENERATE = Link.ofParent(
    LINKS.USER_API_KEYS,
    'generate'
  );

  static USER_API_KEYS_EDIT = Link.ofParent(
    LINKS.USER_API_KEYS,
    'edit/' + p(PARAMS.API_KEY_ID)
  );

  static USER_SETTINGS = Link.ofRoot('user');

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

  static ORGANIZATION_PROJECTS = Link.ofParent(LINKS.ORGANIZATION, 'projects');

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

  static PROJECT_SOCKET_IO_PREVIEW = Link.ofParent(
    LINKS.PROJECT,
    'socket_io_preview'
  );

  static ACTIVITY_PREVIEW = Link.ofParent(LINKS.PROJECT, 'activity');

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
