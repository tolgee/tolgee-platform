import { useState } from 'react';
import {
  Box,
  IconButton,
  Link,
  Popover,
  styled,
  Typography,
} from '@mui/material';
import { HelpCircle } from '@untitled-ui/icons-react';
import { T, TranslationKey } from '@tolgee/react';

import { stopAndPrevent } from 'tg.fixtures/eventHandler';

import { DOCS_LINKS } from 'tg.constants/docLinks';

import { pickLanguageExampleTag, QUALIFIER_HINTS } from './qualifierHints';

const StyledContent = styled(Box)`
  padding: ${({ theme }) => theme.spacing(2)};
  max-width: ${({ theme }) => theme.spacing(50)};
  display: grid;
  gap: ${({ theme }) => theme.spacing(1)};
`;

const StyledExamples = styled('table')`
  border-spacing: 0px ${({ theme }) => theme.spacing(0.5)};
  font-size: ${({ theme }) => theme.typography.body2.fontSize};
`;

const StyledSyntax = styled('td')`
  font-family: monospace;
  white-space: nowrap;
  padding-right: ${({ theme }) => theme.spacing(2)};
  color: ${({ theme }) => theme.palette.primary.main};
`;

const hintNode = (
  hint: { keyName: TranslationKey; defaultValue: string },
  params?: Record<string, string>
) => (
  // @tolgee-ignore
  <T keyName={hint.keyName} defaultValue={hint.defaultValue} params={params} />
);

const buildExamples = (languageExampleTag: string) => [
  {
    syntax: 'description:cart',
    hint: hintNode(QUALIFIER_HINTS.description),
  },
  {
    syntax: 'key:cart*',
    hint: (
      <T
        keyName="translations_search_help_starts_with"
        defaultValue="Key names starting with “cart”"
      />
    ),
  },
  {
    syntax: 'key:*_title',
    hint: (
      <T
        keyName="translations_search_help_ends_with"
        defaultValue="Key names ending with “_title”"
      />
    ),
  },
  {
    syntax: 'namespace:web',
    hint: hintNode(QUALIFIER_HINTS.namespace),
  },
  {
    syntax: 'translation:cart',
    hint: hintNode(QUALIFIER_HINTS.translation),
  },
  {
    syntax: `${languageExampleTag}:cart`,
    hint: hintNode(QUALIFIER_HINTS.language, { tag: languageExampleTag }),
  },
  {
    syntax: '-description:legacy',
    hint: (
      <T
        keyName="translations_search_help_negation"
        defaultValue="Exclude matching keys"
      />
    ),
  },
  {
    syntax: 'key:"two words"',
    hint: (
      <T
        keyName="translations_search_help_phrase"
        defaultValue="Use quotes for phrases with spaces"
      />
    ),
  },
];

type Props = {
  languageTags: string[];
};

export const SearchSyntaxHelp = ({ languageTags }: Props) => {
  const [anchorEl, setAnchorEl] = useState<HTMLButtonElement | null>(null);
  const languageExampleTag = pickLanguageExampleTag(languageTags);
  const examples = buildExamples(languageExampleTag);

  return (
    <>
      <IconButton
        size="small"
        onClick={stopAndPrevent((e) => setAnchorEl(e.currentTarget))}
        onMouseDown={stopAndPrevent()}
        data-cy="translations-search-help-button"
      >
        <HelpCircle width={20} height={20} />
      </IconButton>
      <Popover
        open={Boolean(anchorEl)}
        anchorEl={anchorEl}
        onClose={() => setAnchorEl(null)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'left' }}
        data-cy="translations-search-help-popover"
      >
        <StyledContent>
          <Typography variant="subtitle2">
            <T
              keyName="translations_search_help_title"
              defaultValue="Advanced search"
            />
          </Typography>
          <Typography variant="body2">
            <T
              keyName="translations_search_help_intro"
              defaultValue="Scope your search to specific fields with qualifiers."
            />
          </Typography>
          <StyledExamples>
            <tbody>
              {examples.map((example) => (
                <tr key={example.syntax}>
                  <StyledSyntax>{example.syntax}</StyledSyntax>
                  <td>{example.hint}</td>
                </tr>
              ))}
            </tbody>
          </StyledExamples>
          <Typography variant="body2">
            <Link
              href={DOCS_LINKS.scopedSearch}
              target="_blank"
              rel="noreferrer"
              data-cy="translations-search-help-docs-link"
            >
              <T
                keyName="translations_search_help_docs_link"
                defaultValue="Read more in the documentation"
              />
            </Link>
          </Typography>
        </StyledContent>
      </Popover>
    </>
  );
};
