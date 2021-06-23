const path = require('path');
const { DataCyPlugin } = require('./dataCy.plugin');
const CopyPlugin = require('copy-webpack-plugin');

module.exports = function ({ env: _env }) {
  return {
    typescript: {
      enableTypeChecking: false,
    },
    babel: {
      plugins: ['babel-plugin-transform-typescript-metadata'],
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
