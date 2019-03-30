var utils = require('./utils')
var webpack = require('webpack')
var config = require('../config')
var merge = require('webpack-merge')
var baseWebpackConfig = require('./webpack.base.conf')
var HtmlWebpackPlugin = require('html-webpack-plugin')
var BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;
var FriendlyErrorsPlugin = require('friendly-errors-webpack-plugin')

var devConfig = merge(baseWebpackConfig, {
  module: {
  },
  // cheap-module-eval-source-map is faster for development
  devtool: 'inline-source-map',
  devServer: {
    contentBase: './dist'
  },
  mode: 'development',
  plugins: [
    new webpack.DefinePlugin({
      'process.env': config.dev.env
    }),
    new webpack.NoEmitOnErrorsPlugin(),
    // https://github.com/ampedandwired/html-webpack-plugin
    new HtmlWebpackPlugin({
      filename: 'index.html',
      template: 'index.html',
      inject: true
    }),
    new FriendlyErrorsPlugin(),
    new BundleAnalyzerPlugin({
      openAnalyzer: false,
      logLevel: 'warn'
    })
  ]
})

module.exports = devConfig
