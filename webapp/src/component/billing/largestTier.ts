type TierLike = { id: number; includedWords: number };

/**
 * Whether the customer sits on the biggest size the offering sells, in which case auto-upgrade
 * has nothing to move them to. The API already drops archived tiers from this list (keeping only
 * one the customer is actually on), so there is nothing to filter here.
 */
export function isLargestTier(
  tiers: TierLike[] | undefined,
  // nullable because currentTierId is Long? on both plan models
  tierId: number | null | undefined
): boolean {
  if (!tiers?.length || tierId == null) {
    return false;
  }
  const current = tiers.find((tier) => tier.id === tierId);
  if (!current) {
    return false;
  }
  return tiers.every((tier) => tier.includedWords <= current.includedWords);
}
