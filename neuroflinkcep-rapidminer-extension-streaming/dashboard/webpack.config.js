/*
 Webpack configuration file. Depending on the NODE_ENV, it requires
 the actual config file from ./webpack/webpack.{NODE_ENV}.js.
 */

// Allow to use ES6 syntax in the config files
require('@babel/register');

const env = process.env.NODE_ENV || 'production';

module.exports = require('./webpack/webpack.' + env);
