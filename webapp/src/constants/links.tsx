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
    static ofParent(link: Link, itemTemplate: string): Link {
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
    REPOSITORY_ID = 'repositoryId',
    LANGUAGE_ID = 'languageId',
    API_KEY_ID = 'languageId',
    USER_ID = 'userID',
    VERIFICATION_CODE = 'verificationCode',
}

export class LINKS {

    /**
     * Authentication
     */
    static ROOT = Link.ofRoot('');

    static LOGIN = Link.ofRoot('login');

    static OAUTH_RESPONSE = Link.ofParent(LINKS.LOGIN, 'auth_callback/' + p(PARAMS.SERVICE_TYPE));

    static EMAIL_VERIFICATION = Link.ofParent(LINKS.LOGIN, 'verify_email/' + p(PARAMS.USER_ID) + '/' + p(PARAMS.VERIFICATION_CODE));

    static RESET_PASSWORD_REQUEST = Link.ofRoot('reset_password_request');

    static RESET_PASSWORD = Link.ofRoot('reset_password');

    static RESET_PASSWORD_WITH_PARAMS = Link.ofParent(LINKS.RESET_PASSWORD, p(PARAMS.ENCODED_EMAIL_AND_CODE));

    static SIGN_UP = Link.ofRoot('sign_up');

    static ACCEPT_INVITATION = Link.ofRoot('accept_invitation/' + p(PARAMS.INVITATION_CODE));


    /**
     * Authenticated user stuff
     */

    static USER_API_KEYS = Link.ofRoot('apiKeys');

    static USER_API_KEYS_GENERATE = Link.ofParent(LINKS.USER_API_KEYS, 'generate');

    static USER_API_KEYS_EDIT = Link.ofParent(LINKS.USER_API_KEYS, 'edit/' + p(PARAMS.API_KEY_ID));

    static USER_SETTINGS = Link.ofRoot('user');

    /**
     * Repository stuff
     */

    static REPOSITORIES = Link.ofRoot('repositories');

    /**
     * Visible with view permissions
     */

    static AFTER_LOGIN = LINKS.REPOSITORIES;

    static REPOSITORY = Link.ofParent(LINKS.REPOSITORIES, p(PARAMS.REPOSITORY_ID));

    static REPOSITORY_ADD = Link.ofParent(LINKS.REPOSITORIES, 'add');

    static REPOSITORY_TRANSLATIONS = Link.ofParent(LINKS.REPOSITORY, 'translations');

    /**
     * Visible with edit permissions
     */

    static REPOSITORY_TRANSLATIONS_ADD = Link.ofParent(LINKS.REPOSITORY_TRANSLATIONS, "add");

    /**
     * Visible with manage permissions
     */

    static REPOSITORY_MANAGE = Link.ofParent(LINKS.REPOSITORY, "manage");

    static REPOSITORY_EDIT = Link.ofParent(LINKS.REPOSITORY_MANAGE, "edit");

    static REPOSITORY_LANGUAGES = Link.ofParent(LINKS.REPOSITORY_MANAGE, 'languages');

    static REPOSITORY_LANGUAGE_EDIT = Link.ofParent(LINKS.REPOSITORY_LANGUAGES, 'edit/' + p(PARAMS.LANGUAGE_ID));

    static REPOSITORY_INVITATION = Link.ofParent(LINKS.REPOSITORY_MANAGE, 'invite');

    static REPOSITORY_PERMISSIONS = Link.ofParent(LINKS.REPOSITORY_MANAGE, 'permissions');

    static REPOSITORY_LANGUAGES_CREATE = Link.ofParent(LINKS.REPOSITORY_LANGUAGES, 'add');

    static REPOSITORY_IMPORT = Link.ofParent(LINKS.REPOSITORY, "import");

    static REPOSITORY_EXPORT = Link.ofParent(LINKS.REPOSITORY, "export");
}
