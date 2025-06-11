export function assertUnreachable(x: never): never {
  throw new Error("Didn't expect to get here");
}

export function assertUnreachableReturnNull(x: never): null {
  return null;
}
