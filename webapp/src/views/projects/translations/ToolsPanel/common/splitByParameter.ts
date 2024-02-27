export const arraySplit = <T>(array: T[], callback: (val: T) => any) => {
  const groups: T[][] = [];
  let lastResult: any = undefined;
  array.forEach((i) => {
    const result = callback(i);
    if (result !== lastResult || groups.length === 0) {
      groups.push([]);
    }
    groups[groups.length - 1].push(i);
    lastResult = result;
  });
  return groups;
};
