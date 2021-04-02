const path = require("path");
const ModuleScopePlugin = require("react-dev-utils/ModuleScopePlugin");

module.exports = function ({env: _env}) {
    return {
        babel: {
            plugins: [
                "babel-plugin-transform-typescript-metadata"
            ]
        },
        webpack: {
            alias: {
                react: path.resolve('node_modules/react'),
                "react-dom": path.resolve('node_modules/react-dom'),
            }
        }
    };
};
