export const putBaseLangFirst = (
  languages: string[] | undefined,
  base: string | undefined
) => {
  if (base && languages?.includes(base)) {
    return [base, ...languages.filter((val) => val !== base)];
  }
  return languages;
};
