const path = require("path");
const {DataCyPlugin} = require("./dataCy.plugin");

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
            },
            plugins: [new DataCyPlugin()]
        }
    };
};

