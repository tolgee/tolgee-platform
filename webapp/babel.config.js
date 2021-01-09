module.exports = function (api) {
    api.cache(true);

    return {
        "presets": [
            ["@babel/env", {"targets": "> 0.25%", "useBuiltIns": "usage"}],
            "@babel/preset-react",
            ["@babel/preset-typescript", {"allExtensions": true, "isTSX": true}],

        ],
        "plugins": [
            "@babel/plugin-syntax-dynamic-import",
            ["@babel/plugin-proposal-decorators", {"legacy": true}],
            "@babel/proposal-class-properties",
            "@babel/proposal-object-rest-spread",
            "babel-plugin-transform-typescript-metadata",
            ['babel-plugin-transform-imports',
                {
                    '@material-ui/core': {
                        // Use "transform: '@material-ui/core/${member}'," if your bundler does not support ES modules
                        'transform': '@material-ui/core/esm/${member}',
                        'preventFullImport': true
                    },
                    '@material-ui/icons': {
                        // Use "transform: '@material-ui/icons/${member}'," if your bundler does not support ES modules
                        'transform': '@material-ui/icons/esm/${member}',
                        'preventFullImport': true
                    }
                }
            ]
        ]
    }
};