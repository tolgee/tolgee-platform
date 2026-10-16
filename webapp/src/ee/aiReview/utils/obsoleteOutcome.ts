import { components } from 'tg.service/apiSchema.generated';

type Outcome =
  components['schemas']['TranslationAiReviewSummaryModel']['outcome'];
type State = components['schemas']['TranslationViewModel']['state'];

const SHOWN_ONLY_WHILE_TRANSLATED: Outcome[] = ['BELOW_THRESHOLD', 'FAILED'];

export const isObsoleteForState = (
  outcome: Outcome,
  state: State | undefined
) => SHOWN_ONLY_WHILE_TRANSLATED.includes(outcome) && state !== 'TRANSLATED';
