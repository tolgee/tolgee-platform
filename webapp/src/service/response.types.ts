import { components } from './apiSchema.generated';

export type TranslationsObject = { [abbreviation: string]: string };

export type KeyTranslationsDTO = {
  name: string;
  id: number;
  translations: TranslationsObject;
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

export type ProjectDTO = {
  id: number;
  name: string;
  permissionType: ProjectPermissionType;
};

export interface TokenDTO {
  accessToken: string;
}

export type ErrorResponseDto = {
  CUSTOM_VALIDATION?: { [key: string]: any[] };
  STANDARD_VALIDATION?: { [key: string]: any[] };
  code: string;
  params: any[];
  __handled: boolean;
};

export enum ProjectPermissionType {
  NONE = 'NONE',
  MANAGE = 'MANAGE',
  EDIT = 'EDIT',
  TRANSLATE = 'TRANSLATE',
  VIEW = 'VIEW',
}

export enum OrganizationRoleType {
  MEMBER = 'MEMBER',
  MAINTAINER = 'MAINTAINER',
  OWNER = 'OWNER',
}

export interface InvitationDTO {
  id: number;
  code: string;
  type: ProjectPermissionType;
}

export interface PermissionDTO {
  id: number;
  username: string;
  userId: number;
  userFullName: string;
  type: ProjectPermissionType;
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

export type ApiKeyModel = components['schemas']['ApiKeyModel'];

export interface PermissionEditDTO {
  permissionId: number;
  type: ProjectPermissionType;
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
};
