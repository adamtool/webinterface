import { createVue } from './main.base'

createVue({
  baseUrl: process.env.NODE_ENV === 'development' ? 'http://localhost:4567' : '',
  // ADAMWEB_MODE is an environment variable meant to be defined at build time.
  // we can't access this directly over process.env here because this code runs in the client.
  // ("process.env.NODE_ENV" is a fake environment variable defined through a webpack plugin.)
  // We pass it through to the client using the DefinePlugin in webpack.base.conf.js.
// eslint-disable-next-line no-undef
  mode: ADAMWEB_MODE
})
