import { components } from 'tg.service/apiSchema.generated';

export interface EditApiKeyDTO {
  id: number;
  scopes: string[];
}

export interface ImportDto {
  data: { [key: string]: string };
  languageAbbreviation: string;
}

export interface UserUpdateDTO {
  email: string;
  name: string;
  currentPassword?: string;
  profileVisibility?: components['schemas']['UserUpdateRequestDto']['profileVisibility'];
}

export interface UserUpdatePasswordDTO {
  currentPassword: string;
  password: string;
}
