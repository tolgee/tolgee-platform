import Anthropic from '@anthropic-ai/sdk'
import {
  ANTHROPIC_API_KEY,
  ANTHROPIC_MODEL_DETECT,
  ANTHROPIC_MODEL_SUGGEST,
} from './config'

export type DetectInput = {
  baseLang: string
  baseText: string
  languageTag: string
  text: string
}

export type Detection = {
  /** Whether this change looks like it uses a term that should be in the glossary. */
  suspicious: boolean
  /** The term in question, in the base language (empty when not suspicious). */
  candidateTerm: string
  reason: string
}

export type SuggestInput = DetectInput & { candidateTerm: string }

export type GlossarySuggestion = {
  /** Canonical term, in the base language. */
  term: string
  /** Suggested translation of the term in the changed language. */
  translation: string
  description: string
}

const client = ANTHROPIC_API_KEY
  ? new Anthropic({ apiKey: ANTHROPIC_API_KEY })
  : null

export const isAnthropicConfigured = (): boolean => client !== null

/** Pulls the first JSON object out of a model response, tolerating markdown fences. */
export const extractJson = <T>(raw: string): T => {
  const cleaned = raw.replace(/```(?:json)?/g, '').trim()
  const start = cleaned.indexOf('{')
  const end = cleaned.lastIndexOf('}')
  if (start < 0 || end < 0) throw new Error('No JSON in model response')
  return JSON.parse(cleaned.slice(start, end + 1)) as T
}

const firstText = (content: Anthropic.Messages.ContentBlock[]): string => {
  const block = content[0]
  return block && block.type === 'text' ? block.text : ''
}

const detectPrompt = (input: DetectInput): string =>
  `You maintain a translation glossary. A translator just changed a translation.

Base (${input.baseLang}): "${input.baseText}"
Changed (${input.languageTag}): "${input.text}"

Decide whether this change hinges on a domain/product/brand TERM that should be
defined in the glossary so it is translated consistently — e.g. a feature name,
technical term, or term of art. Ignore ordinary words and pure rephrasing.

Respond ONLY with valid JSON, no preamble, no markdown:
{
  "suspicious": true | false,
  "candidateTerm": "the term in ${input.baseLang}, or empty string",
  "reason": "one short sentence"
}`

const suggestPrompt = (input: SuggestInput): string =>
  `Draft a glossary entry for the term "${input.candidateTerm}".

Context — base (${input.baseLang}): "${input.baseText}"
Context — ${input.languageTag}: "${input.text}"

Respond ONLY with valid JSON, no preamble, no markdown:
{
  "term": "canonical term in ${input.baseLang}",
  "translation": "recommended ${input.languageTag} translation of the term",
  "description": "one short sentence explaining the term"
}`

/** Cheap first pass (Haiku): is this change worth a glossary entry? */
export async function detectSuspicious(input: DetectInput): Promise<Detection> {
  if (!client) throw new Error('ANTHROPIC_API_KEY is not set')
  const response = await client.messages.create({
    model: ANTHROPIC_MODEL_DETECT,
    max_tokens: 300,
    messages: [{ role: 'user', content: detectPrompt(input) }],
  })
  return extractJson<Detection>(firstText(response.content))
}

/** Second pass (Sonnet): draft the actual glossary entry. */
export async function suggestGlossaryEntry(
  input: SuggestInput
): Promise<GlossarySuggestion> {
  if (!client) throw new Error('ANTHROPIC_API_KEY is not set')
  const response = await client.messages.create({
    model: ANTHROPIC_MODEL_SUGGEST,
    max_tokens: 400,
    messages: [{ role: 'user', content: suggestPrompt(input) }],
  })
  return extractJson<GlossarySuggestion>(firstText(response.content))
}
