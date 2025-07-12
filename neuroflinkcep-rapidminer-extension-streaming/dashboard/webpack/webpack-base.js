const path = require('path');
const _ = require('lodash');
const webpack = require('webpack');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const { CleanWebpackPlugin } = require('clean-webpack-plugin');

// Pass through Node ENV variables to the app
const passThroughEnvVars = (args) =>
	_.reduce(
		args,
		(result, arg) => {
			const env = process.env[arg];
			/* eslint-disable no-param-reassign */
			result[arg] = env && JSON.stringify(env);
			/* eslint-enable no-param-reassign */
			return result;
		},
		{}
	);

const baseDir = path.resolve(__dirname, '..');

const config = {
	context: path.resolve(baseDir, 'src', 'ts'),
	entry: {
		frontend: ['core-js/stable', 'regenerator-runtime/runtime', path.resolve(baseDir, 'src', 'ts', 'index.tsx')]
	},
	output: {
		path: path.resolve(baseDir, '..', 'src/main/resources/com/rapidminer/extension/resources/html5/dashboard/dist'),
		filename: '[name].bundle.js',
		chunkFilename: '[name].bundle.js',
		publicPath: '.'
	},
	optimization: {
		runtimeChunk: true
	},
	performance: {
		hints: false
	},
	plugins: [
		// https://github.com/webpack/webpack/issues/6556
		new webpack.LoaderOptionsPlugin({ options: {} }),
		new MiniCssExtractPlugin({
			// Options similar to the same options in webpackOptions.output
			// both options are optional
			filename: '[name].bundle.css',
			chunkFilename: '[name].bundle.css'
		}),
		new webpack.DefinePlugin({
			'process.env': passThroughEnvVars(['NODE_ENV'])
		}),
		new webpack.IgnorePlugin(/^\.\/locale$/, /moment$/),
		new CleanWebpackPlugin({ cleanStaleWebpackAssets: false }),
		new HtmlWebpackPlugin({ template: path.resolve(baseDir, 'src/template/index.html') })
	],
	resolve: {
		modules: [path.resolve(baseDir, 'src/js'), path.resolve(baseDir, 'src/stylesheets'), 'node_modules'],
		extensions: ['.ts', '.tsx', '.js']
	}
};

export default config;
