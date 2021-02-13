const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const CopyPlugin = require('copy-webpack-plugin');
const webpack = require("webpack");
const dotenv = require('dotenv');

module.exports = env => {
    const isDevelopment = env.mode === "development";
    const mode = env.mode || 'production';

    const dotenvMaster = dotenv.config({path: ".env"});
    const dotenvProfile = dotenv.config({path: ".env." + (isDevelopment ? "dev" : "prod")});
    env = {...dotenvProfile.parsed, ...dotenvMaster.parsed, ...env, ...process.env};

    return {
        entry: {
            index: "./src/index.tsx",
        },
        devtool: 'source-map',
        output: {
            filename: !isDevelopment ? '[name].[chunkhash].js' : '[name].[hash].js',
            chunkFilename: !isDevelopment ? '[name].[chunkhash].js' : '[name].[hash].js',
            path: path.resolve(__dirname, 'dist'),
            publicPath: '/',
        },
        resolve: {
            extensions: [".webpack.js", ".web.js", ".ts", ".js", ".tsx"],
            symlinks: false
        },
        module: {
            rules: [
                {
                    test: /\.m?js/,
                    resolve: {
                        fullySpecified: false
                    }
                },
                {
                    test: /\.jsx?$/,
                    enforce: 'pre',
                    use: ['source-map-loader']
                },
                {
                    test: /\.tsx?$/,
                    use: ["ts-loader"],
                    exclude: [/node_modules/, /lib/],
                }, {
                    test: /\.svg$/,
                    use: ["babel-loader",
                        {
                            loader: "react-svg-loader",
                            options: {
                                jsx: true // true outputs JSX tags
                            }
                        }
                    ]
                },
                {
                    test: /\.(woff(2)?|ttf|eot)(\?v=\d+\.\d+\.\d+)?$/,
                    use: [
                        {
                            loader: 'file-loader',
                            options: {
                                name: '[name].[ext]',
                                outputPath: 'fonts/'
                            }
                        }
                    ]
                },
                {
                    test: /favicon\.svg/,
                    use: ['file-loader?name=[name].[ext]']
                },
            ]
        },
        optimization: {
            usedExports: true,
            splitChunks: {
                chunks: 'all'
            },
        },
        mode,
        plugins: [
            new HtmlWebpackPlugin({
                favicon: "./src/favicon.svg",
                template: './src/index.html',
            }),
            new CopyPlugin({
                patterns: [
                    {from: 'public', to: '.'}
                ]
            }),
            new webpack.DefinePlugin({
                "environment": JSON.stringify({
                    mode: env.mode,
                    sentryDsn: env.sentryDsn,
                    apiUrl: env.apiUrl,
                    tolgeeApiKey: env.tolgeeApiKey,
                    tolgeeApiUrl: env.tolgeeApiUrl,
                    tolgeeWithUI: env.tolgeeWithUI
                })
            })
        ],
        devServer: {
            host: env.host || "localhost",
            historyApiFallback: true,
            overlay: true,
            port: env.port || undefined
        }
    }
};
