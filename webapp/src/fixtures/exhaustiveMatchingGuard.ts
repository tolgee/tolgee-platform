export const exhaustiveMatchingGuard = (_: never): never => {
  throw new Error('Unexhaustive switch statement');
};
