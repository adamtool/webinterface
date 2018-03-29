// Import all the preset APT examples for users to try out.
// See https://webpack.js.org/guides/dependency-management/#require-with-expression
const aptFilesContext = require.context('./assets/apt-examples', true, /\.apt$/)
const aptFiles = {}

function importAll (r) {
  r.keys().forEach(key => {
    aptFiles[key] = r(key)
  })
}

importAll(aptFilesContext)
export default aptFiles
