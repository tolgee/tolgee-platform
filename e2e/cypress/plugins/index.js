/// <reference types="cypress" />
// ***********************************************************
// This example plugins/index.js can be used to load plugins
//
// You can change the location of this file or turn off loading
// the plugins file with the 'pluginsFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/plugins-guide
// ***********************************************************

// This function is called when a project is opened or re-opened (e.g. due to
// the project's config changing)

const dotenvPlugin = require('cypress-dotenv');
const { isFileExist } = require('cy-verify-downloads');
const path = require('path');
const unzipping = require('./unzipping');
const { readFile } = require('xlsx');

/**
 * @type {Cypress.PluginConfig}
 */
module.exports = (on, config) => {
  on('task', {
    isFileExist,
    unzipping: unzipping.unzip,
    readXlsx: readFile,
  });

  return dotenvPlugin(config, {
    path: path.resolve(__dirname, '../../.test.docker.env'),
  });
};
