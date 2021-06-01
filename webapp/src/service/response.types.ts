import { components } from './apiSchema';

export type TranslationsObject = { [abbreviation: string]: string };

export type KeyTranslationsDTO = {
  name: string;
  id: number;
  translations: TranslationsObject;
};

export type LanguageDTO = {
  abbreviation: string;
  id: number;
  name: string;
};
export type TranslationsDataResponse = {
  paginationMeta: {
    offset: number;
    allCount: number;
  };
  params: {
    search: string;
    languages: string[];
  };
  data: KeyTranslationsDTO[];
};

export type RepositoryDTO = {
  id: number;
  name: string;
  permissionType: RepositoryPermissionType;
};

export interface RemoteConfigurationDTO {
  clientSentryDsn: string;
  authentication: boolean;
  passwordResettable: boolean;
  allowRegistrations: boolean;
  authMethods: {
    github: {
      enabled: boolean;
      clientId: string;
    };
  };
  screenshotsUrl: string;
  maxUploadFileSize: number;
  needsEmailVerification: boolean;
  userCanCreateOrganizations: boolean;
  userCanCreateRepositories: boolean;
}

export interface TokenDTO {
  accessToken: string;
}

export type ErrorResponseDTO = {
  CUSTOM_VALIDATION?: { [key: string]: any[] };
  STANDARD_VALIDATION?: { [key: string]: any[] };
  code: string;
  params: any[];
  __handled: boolean;
};

export enum RepositoryPermissionType {
  MANAGE = 'MANAGE',
  EDIT = 'EDIT',
  TRANSLATE = 'TRANSLATE',
  VIEW = 'VIEW',
}

export enum OrganizationRoleType {
  MEMBER = 'MEMBER',
  OWNER = 'OWNER',
}

export interface InvitationDTO {
  id: number;
  code: string;
  type: RepositoryPermissionType;
}

export interface PermissionDTO {
  id: number;
  username: string;
  userId: number;
  userFullName: string;
  type: RepositoryPermissionType;
}

export interface UserDTO {
  id: number;
  username: string;
  name: string;
  emailAwaitingVerification?: string;
}

export interface UserUpdateDTO {
  email: string;
  name: string;
  password?: string;
}

export interface ApiKeyDTO {
  id: number;
  key: string;
  userName: string;
  scopes: string[];
  repositoryId: number;
  repositoryName: string;
}

export interface PermissionEditDTO {
  permissionId: number;
  type: RepositoryPermissionType;
}

export interface ScreenshotDTO {
  id: 0;
  filename: string;
  createdAt: string;
}

export type HateoasPaginatedData<ItemDataType> = {
  page?: components['schemas']['PageMetadata'];
} & HateoasListData<ItemDataType>;

export type HateoasListData<ItemDataType> = {
  _embedded?: { [key: string]: ItemDataType[] };
  _links?: components['schemas']['Links'];
};
