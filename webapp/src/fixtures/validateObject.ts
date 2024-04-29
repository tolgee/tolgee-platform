export const validateObject = (value: any) => {
  if (!value) {
    return false;
  }
  try {
    const parsed = JSON.parse(value);
    return Boolean(
      typeof parsed === 'object' && parsed !== null && !Array.isArray(parsed)
    );
  } catch (e) {
    return false;
  }
};
