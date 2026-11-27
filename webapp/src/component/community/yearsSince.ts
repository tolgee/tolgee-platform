export const yearsSince = (
  starts: (number | undefined)[],
  now = new Date()
): number[] => {
  const currentYear = now.getFullYear();
  const firstYear = Math.min(
    currentYear,
    ...starts
      .filter((s): s is number => s !== undefined)
      .map((s) => new Date(s).getUTCFullYear())
  );
  return Array.from(
    { length: currentYear - firstYear + 1 },
    (_, i) => currentYear - i
  );
};
