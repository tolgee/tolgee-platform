const path = require("path");
const ModuleScopePlugin = require("react-dev-utils/ModuleScopePlugin");

module.exports = function ({ env: _env }) {
    return {
        babel: {
            plugins: [
                "babel-plugin-transform-typescript-metadata"
            ]
        },
    };
};