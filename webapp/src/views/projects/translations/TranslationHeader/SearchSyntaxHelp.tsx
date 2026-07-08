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
import { T } from '@tolgee/react';

import { stopAndPrevent } from 'tg.fixtures/eventHandler';

const StyledContent = styled(Box)`
  padding: ${({ theme }) => theme.spacing(2)};
  max-width: 400px;
  display: grid;
  gap: ${({ theme }) => theme.spacing(1)};
`;

const StyledExamples = styled('table')`
  border-spacing: 0px ${({ theme }) => theme.spacing(0.5)};
  font-size: 14px;
`;

const StyledSyntax = styled('td')`
  font-family: monospace;
  white-space: nowrap;
  padding-right: ${({ theme }) => theme.spacing(2)};
  color: ${({ theme }) => theme.palette.primary.main};
`;

const EXAMPLES = [
  {
    syntax: 'description:cart',
    hint: (
      <T
        keyName="translations_search_help_description"
        defaultValue="Search only in key descriptions"
      />
    ),
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
    hint: (
      <T
        keyName="translations_search_help_namespace"
        defaultValue="Search only in namespaces"
      />
    ),
  },
  {
    syntax: 'translation:cart',
    hint: (
      <T
        keyName="translations_search_help_translation"
        defaultValue="Search only in translation texts"
      />
    ),
  },
  {
    syntax: 'de:Warenkorb',
    hint: (
      <T
        keyName="translations_search_help_language"
        defaultValue="Search only in one language"
      />
    ),
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

export const SearchSyntaxHelp = () => {
  const [anchorEl, setAnchorEl] = useState<HTMLButtonElement | null>(null);

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
              {EXAMPLES.map((example) => (
                <tr key={example.syntax}>
                  <StyledSyntax>{example.syntax}</StyledSyntax>
                  <td>{example.hint}</td>
                </tr>
              ))}
            </tbody>
          </StyledExamples>
          <Typography variant="body2">
            <Link
              href="https://docs.tolgee.io/platform/translation_process/scoped_search"
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
