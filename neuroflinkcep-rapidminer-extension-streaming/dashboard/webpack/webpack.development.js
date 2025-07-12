import WriteFilePlugin from 'write-file-webpack-plugin';
import webpack from 'webpack';

import base from './webpack-base';
import rules from './webpack-rules';

const ReactRefreshWebpackPlugin = require('@pmmmwh/react-refresh-webpack-plugin');

const devEntry = {
	frontend: [...base.entry.frontend, 'webpack-hot-middleware/client?reload=true&noInfo=true&path=/__webpack_hmr']
};

export default {
	...base,
	mode: 'development',
	entry: devEntry,
	devtool: 'cheap-module-eval-source-map',
	watch: true,
	watchOptions: {
		ignored: [/node_modules/, /dev/, /build/]
	},
	module: {
		strictExportPresence: true,
		rules: [
			...rules(),
			{
				enforce: 'pre',
				test: /\.(ts)x?$/,
				loader: 'eslint-loader',
				exclude: /node_modules/,
				options: {
					// Do not emit errors, it breaks hot loading of components.
					emitError: false,
					emitWarning: true
				}
			}
		]
	},
	plugins: [
		...base.plugins,
		new WriteFilePlugin({
			force: true,
			exitOnErrors: false
		}),
		new webpack.HotModuleReplacementPlugin(),
		new ReactRefreshWebpackPlugin()
	]
};
