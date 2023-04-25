export type MergeTypes<A, B> = {
  [P in keyof (A & B)]: P extends keyof (A | B)
    ? (A & B)[P]
    : (A & B)[P] | undefined;
};
