export const isMenuItemSelected = (
  pathname: string,
  linkTo: string,
  matchAsPrefix?: boolean
) => {
  if (!matchAsPrefix) {
    return pathname === linkTo;
  }
  return pathname === linkTo || pathname.startsWith(`${linkTo}/`);
};
