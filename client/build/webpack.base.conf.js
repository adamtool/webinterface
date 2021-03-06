var path = require('path')
var utils = require('./utils')
var config = require('../config')
var DirectoryTreePlugin = require('directory-tree-webpack-plugin')
var VueLoaderPlugin = require('vue-loader/lib/plugin')
var MiniCssExtractPlugin = require('mini-css-extract-plugin')
var webpack = require('webpack')
var childProcess = require('child_process')

function resolve (dir) {
  return path.join(__dirname, '..', dir)
}

module.exports = {
  entry: {
    app: './src/main.js'
  },
  output: {
    path: config.build.assetsRoot,
    filename: '[name].js',
    publicPath: process.env.NODE_ENV === 'production'
      ? config.build.assetsPublicPath
      : config.dev.assetsPublicPath
  },
  resolve: {
    extensions: ['.js', '.vue', '.json'],
    alias: {
      'vue$': 'vue/dist/vue.esm.js',
      '@': resolve('src')
    }
  },
  plugins: [
    new webpack.DefinePlugin({
      '____ADAM_WEB_BUILD_DATE____': `"${new Date().toString()}"`,
      '____ADAM_WEB_BUILD_SHA____': `"${childProcess.execSync('git rev-parse HEAD').toString().replace('\n', '')}"`
    }),
    new DirectoryTreePlugin({
      dir: './src/assets/apt-examples',
      path: './src/assets/apt-examples.json',
      extensions: /\.apt/
    }),
    new VueLoaderPlugin()
  ],
  module: {
    rules: [
      {
        test: /\.vue$/,
        loader: 'vue-loader'
        // ,
        // options: vueLoaderConfig
      },
      {
        test: /\.js$/,
        loader: 'babel-loader',
        include: [resolve('src'), resolve('test')]
      },
      {
        test: /\.css$/,
        use: [
          process.env.NODE_ENV !== 'production'
            ? 'vue-style-loader'
            : MiniCssExtractPlugin.loader,
          'css-loader'
        ]
      },
      {
        test: /\.(js|vue)$/,
        loader: 'eslint-loader',
        enforce: 'pre',
        exclude: /node_modules/,
        options: {
          formatter: require('eslint-friendly-formatter')
        }
      },
      {
        test: /\.(png|jpe?g|gif|svg)(\?.*)?$/,
        loader: 'url-loader',
        options: {
          limit: 10000,
          name: utils.assetsPath('img/[name].[hash:7].[ext]')
        }
      },
      {
        test: /\.(mp4|webm|ogg|mp3|wav|flac|aac)(\?.*)?$/,
        loader: 'url-loader',
        options: {
          limit: 10000,
          name: utils.assetsPath('media/[name].[hash:7].[ext]')
        }
      },
      {
        test: /\.(woff2?|eot|ttf|otf)(\?.*)?$/,
        loader: 'url-loader',
        options: {
          limit: 10000,
          name: utils.assetsPath('fonts/[name].[hash:7].[ext]')
        }
      },
      {
        test: /\.apt$/,
        use: 'raw-loader'
      }
    ]
  }
}
