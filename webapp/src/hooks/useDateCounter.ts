export const useDateCounter = () => {
  let previousDate: Date | undefined;

  const isNewDate = (date: Date) => {
    const previous = previousDate?.toLocaleDateString();
    previousDate = date;
    return previous !== date.toLocaleDateString();
  };

  return {
    isNewDate,
  };
};
