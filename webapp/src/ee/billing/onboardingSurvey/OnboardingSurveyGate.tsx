/**
 * Stub onboarding survey gate — used when the billing repo is not present.
 * When the billing repo exists, Vite's alias overrides this with the real
 * implementation from billing/frontend/src/onboardingSurvey/OnboardingSurveyGate.tsx.
 */
import { ReactElement, ReactNode } from 'react';

export const OnboardingSurveyGate = ({
  children,
}: {
  children: ReactNode;
}): ReactElement | null => <>{children}</>;
