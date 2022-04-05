const path = require('path');
const { DataCyPlugin } = require('./dataCy.plugin');
const CopyPlugin = require('copy-webpack-plugin');

const CracoAlias = require('craco-alias');

module.exports = function ({ env: _env }) {
  return {
    plugins: [
      {
        plugin: CracoAlias,
        options: {
          source: 'tsconfig',
          baseUrl: './src',
          tsConfigPath: './tsconfig.extend.json',
        },
      },
    ],
    typescript: {
      enableTypeChecking: false,
    },
    babel: {
      plugins: [
        'babel-plugin-transform-typescript-metadata',
        [
          '@emotion',
          {
            importMap: {
              '@mui/system': {
                styled: {
                  canonicalImport: ['@emotion/styled', 'default'],
                  styledBaseImport: ['@mui/system', 'styled'],
                },
              },
              '@mui/material/styles': {
                styled: {
                  canonicalImport: ['@emotion/styled', 'default'],
                  styledBaseImport: ['@mui/material/styles', 'styled'],
                },
              },
            },
          },
        ],
      ],
    },
    webpack: {
      alias: {
        react: path.resolve('node_modules/react'),
        'react-dom': path.resolve('node_modules/react-dom'),
      },
      plugins: [
        new DataCyPlugin(),
        new CopyPlugin({
          patterns: [
            {
              from: 'node_modules/@tginternal/language-util/flags',
              to: 'static/flags',
            },
          ],
        }),
      ],
    },
  };
};
