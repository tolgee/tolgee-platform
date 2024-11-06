export const getFormatedDate = () => {
  const currentDate = new Date();

  const options = {
    weekday: 'short',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  };

  return currentDate.toLocaleString('en-US', options);
};

export const getShortDate = (timestamp) => {
  const date = new Date(timestamp * 1000);

  const options = {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
  };

  return date.toLocaleString('en-US', options);
};
