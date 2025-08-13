import path from "node:path";

export function pathToPosix(input: string) {
  return input.replaceAll(path.sep, path.posix.sep);
}
