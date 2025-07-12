/* eslint-disable @typescript-eslint/ban-ts-comment */
import express from 'express';
import path from 'path';
import webpack from 'webpack';
import webpackDevMiddleware from 'webpack-dev-middleware';
import webpackHotMiddleware from 'webpack-hot-middleware';

import webpackConfig from '../../../webpack/webpack.development';

const webpackDevMiddlewareConfig = {
	publicPath: webpackConfig.output.publicPath,
	stats: {
		assets: false,
		cached: false,
		children: false,
		chunkModules: false,
		chunks: false,
		colors: true,
		hash: false,
		modules: false,
		reasons: false,
		timings: false,
		version: false,
		lazy: false
	},
	hot: true
};

const app = express();

// @ts-ignore
const compiler = webpack(webpackConfig);
const port = process.env.PORT || 3000;

app.use(webpackDevMiddleware(compiler, webpackDevMiddlewareConfig));
app.use(webpackHotMiddleware(compiler, { path: '/__webpack_hmr' }));
app.use(
	express.static(
		path.resolve(
			__dirname,
			'../../../..',
			'src/main/resources/com/rapidminer/extension/resources/html5/dashboard/dist'
		)
	)
);

app.listen(port, () => {
	// eslint-disable-next-line no-console
	console.log(`Streaming Dashboard dev is now running at http://localhost:${port}`);
});
