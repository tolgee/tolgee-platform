import type { ReactNode } from 'react';
import {
  Box,
  IconButton,
  InputAdornment,
  InputLabel,
  MenuItem,
  OutlinedInput,
  Select,
  TextField,
} from '@mui/material';
import {
  ChevronDown,
  Copy06,
  HelpCircle,
  SearchSm,
  XClose,
} from '../../src/icons';
import { DocsTheme } from './DocsTheme';

/** The product's shape: label above, outlined box, 64px reserved for a message. */
export const Field = ({
  label,
  children,
}: {
  label?: ReactNode;
  children: ReactNode;
}) => (
  <Box sx={{ display: 'grid', minHeight: 64 }}>
    {label && (
      <InputLabel sx={{ fontSize: 14, fontWeight: 400, mb: 0.5 }}>
        {label}
      </InputLabel>
    )}
    {children}
  </Box>
);

export const Sample = ({
  name,
  where,
  built,
  problem,
  children,
}: {
  name: string;
  where: string;
  built: string;
  problem?: string;
  children: ReactNode;
}) => (
  <DocsTheme>
    <Box
      sx={{
        display: 'grid',
        gridTemplateColumns: 'minmax(260px, 300px) 1fr',
        gap: 3,
        alignItems: 'start',
        py: 2,
        borderBottom: (theme) => `1px solid ${theme.palette.divider}`,
      }}
    >
      <Box>{children}</Box>
      <Box>
        <Box sx={{ typography: 'body2' }}>{name}</Box>
        <Box sx={{ typography: 'caption', color: 'text.secondary' }}>
          {where}
        </Box>
        <Box
          sx={{
            typography: 'caption',
            fontFamily: 'monospace',
            color: 'text.secondary',
            mt: 0.5,
          }}
        >
          {built}
        </Box>
        {problem && (
          <Box sx={{ typography: 'caption', color: 'error.main', mt: 0.5 }}>
            {problem}
          </Box>
        )}
      </Box>
    </Box>
  </DocsTheme>
);

/** Rebuilt from searchFieldStyles.ts — a div around a CodeMirror instance. */
export const TranslationsSearch = ({ value }: { value?: string }) => (
  <Box
    sx={{
      display: 'flex',
      alignItems: 'center',
      gap: 0.75,
      height: 40,
      px: 1,
      pl: 1.5,
      borderRadius: (theme) => `${theme.shape.borderRadius}px`,
      border: (theme) => `1px solid ${theme.palette.tokens.border.primary}`,
      background: (theme) => theme.palette.background.default,
      color: 'text.primary',
    }}
  >
    <SearchSm width={20} height={20} />
    <Box
      sx={{
        flex: 1,
        typography: 'body2',
        color: value ? 'text.primary' : 'text.secondary',
      }}
    >
      {value ?? 'Search'}
    </Box>
    {value && (
      <IconButton size="small" aria-label="Clear search">
        <XClose width={20} height={20} />
      </IconButton>
    )}
    <IconButton size="small" aria-label="Search syntax help">
      <HelpCircle width={20} height={20} />
    </IconButton>
  </Box>
);

export const SharedSearch = ({ width = 250 }: { width?: number }) => (
  <TextField
    size="small"
    placeholder="Search"
    defaultValue="homepage"
    sx={{ width }}
    InputProps={{
      startAdornment: (
        <InputAdornment position="start">
          <SearchSm width={20} height={20} />
        </InputAdornment>
      ),
      endAdornment: (
        <InputAdornment position="end" style={{ marginRight: -5 }}>
          <IconButton size="small" aria-label="Clear search">
            <XClose width={20} height={20} />
          </IconButton>
        </InputAdornment>
      ),
    }}
  />
);

/** A TextField that is not a field: readOnly, a div for an input, a menu on click. */
export const FakeSelect = ({ placeholder }: { placeholder: string }) => (
  <TextField
    size="small"
    sx={{ minWidth: 200 }}
    InputProps={{
      readOnly: true,
      sx: { cursor: 'pointer' },
      endAdornment: (
        <InputAdornment position="end">
          <ChevronDown width={20} height={20} />
        </InputAdornment>
      ),
    }}
    placeholder={placeholder}
  />
);

export const CopyField = () => (
  <OutlinedInput
    size="small"
    readOnly
    defaultValue="https://app.tolgee.io/v2/…"
    sx={{ width: 300 }}
    endAdornment={
      <InputAdornment position="end">
        <IconButton size="small" aria-label="Copy">
          <Copy06 width={20} height={20} />
        </IconButton>
      </InputAdornment>
    }
  />
);

export const PlainSelect = ({ value }: { value: string }) => (
  <Select size="small" value={value} fullWidth>
    <MenuItem value={value}>{value}</MenuItem>
  </Select>
);
