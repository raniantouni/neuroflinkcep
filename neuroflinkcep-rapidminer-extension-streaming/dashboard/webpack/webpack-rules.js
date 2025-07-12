const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const isDevelopment = process.env.NODE_ENV !== 'production';
const path = require('path');

export default () => {
	return [
		{
			test: /\.(ts)x?$/,
			exclude: [/node_modules/],
			use: [
				{
					loader: 'babel-loader',
					options: {
						plugins: [isDevelopment && require.resolve('react-refresh/babel')].filter(Boolean)
					}
				}
			]
		},
		{
			test: /\.less$/,
			exclude: /\.module\.less$/,
			use: [
				{
					loader: MiniCssExtractPlugin.loader,
					options: {
						// only enable hot in development
						hmr: process.env.NODE_ENV === 'development',
						// if hmr does not work, this is a forceful method.
						reloadAll: true
					}
				},
				{
					loader: 'css-loader' // translates CSS into CommonJS
				},
				'postcss-loader',
				{
					loader: 'less-loader', // compiles Less to CSS
					options: {
						lessOptions: {
							modifyVars: {
								hack: `true; @import "${path.resolve(
									__dirname,
									'../',
									'src',
									'stylesheets',
									'antd-theme-vars-override.less'
								)}";`
							},
							javascriptEnabled: true
						}
					}
				}
			]
		},
		{
			test: /\.module.less$/,
			use: [
				{
					loader: MiniCssExtractPlugin.loader,
					options: {
						// only enable hot in development
						hmr: process.env.NODE_ENV === 'development',
						// if hmr does not work, this is a forceful method.
						reloadAll: true
					}
				},
				{
					loader: 'css-loader', // translates CSS into CommonJS
					options: {
						sourceMap: false,
						modules: {
							localIdentName: '[name]__[local]--[hash:base64:5]'
						}
					}
				},
				'postcss-loader',
				{
					loader: 'less-loader', // compiles Less to CSS
					options: {
						lessOptions: {
							javascriptEnabled: true
						}
					}
				}
			]
		},
		{
			test: /\.css$/,
			use: [
				'style-loader',
				{
					loader: 'css-loader'
				}
			]
		},
		{
			// file loader for assets and resources (mainly custom icons)
			test: /\.(png|jpg|gif|ico|svg)$/,
			exclude: [/node_modules/],
			use: [
				{
					loader: 'file-loader',
					options: {
						publicPath: '.'
					}
				}
			]
		},
		{
			// file loader for assets and resources (mainly custom icons)
			test: /\.(woff(2)?|ttf|eot|svg)(\?v=\d+\.\d+\.\d+)?$/,
			exclude: [/src/],
			use: [
				{
					loader: 'file-loader',
					options: {
						publicPath: './fonts',
						outputPath: (url) => {
							return `fonts/${url}`;
						},
						esModule: false
					}
				}
			]
		}
	];
};
