export type KeyFormType = {
  name: string;
  namespace: string;
  description: string | undefined;
  tags: string[];
  disabledLangs: number[];
  isPlural: boolean;
  pluralParameter: string;
  custom: string;
};
