import { describe, expect, it } from 'vitest';

import { billingLinkFor } from 'tg.fixtures/billingLink';

describe('billingLinkFor', () => {
  it('sends a cloud organization straight to the page that sells plans', () => {
    expect(billingLinkFor({ slug: 'acme-inc' })).toBe(
      '/organizations/acme-inc/subscriptions'
    );
  });

  it('sends a self-hosted organization to its self-hosted subscriptions page', () => {
    expect(billingLinkFor({ slug: 'acme-inc', selfHosted: true })).toBe(
      '/organizations/acme-inc/subscriptions/self-hosted-ee'
    );
  });
});
