// http://eslint.org/docs/user-guide/configuring

module.exports = {
  root: true,
  parserOptions: {
    parser: 'babel-eslint',
    sourceType: 'module'
  },
  env: {
    browser: true,
  },
  // TODO Consider using vue/recommended linting rules
  // 'extends': 'plugin:vue/recommended',
  'extends': 'plugin:vue/base',
  // required to lint *.vue files
  plugins: [
    'vue'
  ],
  // add your custom rules here
  'rules': {
    // allow paren-less arrow functions
    'arrow-parens': 0,
    // allow async-await
    'generator-star-spacing': 0,
    // allow debugger during development
    'no-debugger': process.env.NODE_ENV === 'production' ? 2 : 0,
    'no-unneeded-ternary': 0,
    'no-unused-vars': 0,
    'object-curly-spacing': 0
  }
}
