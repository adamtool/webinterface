var path = require('path')
var utils = require('./utils')
var config = require('../config')
var vueLoaderConfig = require('./vue-loader.conf')
var DirectoryTreePlugin = require('directory-tree-webpack-plugin')
var webpack = require('webpack')

function resolve (dir) {
  return path.join(__dirname, '..', dir)
}

const mode = process.env.ADAMWEB_MODE
const validModes = ['MODEL_CHECKING', 'OTHER_APPROACH', 'MODEL_CHECKING_AND_OTHER_APPROACH']
if (!(validModes.includes(mode))) {
  throw new Error(`The environment variable ADAMWEB_MODE has an invalid value: ${mode}
  Valid modes: [${validModes.join(', ')}]`)
}
const modeQuoted = `'${mode}'`

console.log(`ADAMWEB_MODE: ${mode}`)

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
    new DirectoryTreePlugin({
      dir: './src/assets/apt-examples',
      path: './src/assets/apt-examples.json',
      extensions: /\.apt/
    }),
    new webpack.DefinePlugin({
      // Read these environment variables and perform a full-text find-and-replace on our source code,
      // replacing occurrences of the env variables' names with their values at compile time.
      ADAMWEB_MODE: modeQuoted
    })
  ],
  module: {
    rules: [
      {
        test: /\.(js|vue)$/,
        loader: 'eslint-loader',
        enforce: 'pre',
        include: [resolve('src'), resolve('test')],
        options: {
          formatter: require('eslint-friendly-formatter')
        }
      },
      {
        test: /\.vue$/,
        loader: 'vue-loader',
        options: vueLoaderConfig
      },
      {
        test: /\.js$/,
        loader: 'babel-loader',
        include: [resolve('src'), resolve('test')]
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
