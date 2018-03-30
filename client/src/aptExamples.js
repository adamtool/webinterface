// Import all the preset APT examples for users to try out.
// See https://webpack.js.org/guides/dependency-management/#require-with-expression
const aptFilesContext = require.context('./assets/apt-examples', true, /\.apt$/)
const aptFileTree = {
  type: 'directory',
  name: 'root',
  children: []
}

importAll(aptFilesContext)
export default aptFileTree

function importAll (r) {
  r.keys().forEach(key => {
    // Use substring to discard the './' at the start of each path
    importPath(key.substring(2), r(key), aptFileTree)
  })
}

function importPath (path, fileContents, tree) {
  const positionSlash = path.indexOf('/')
  if (positionSlash === -1) {
    tree.children.push({
      type: 'aptFile',
      name: path,
      body: fileContents
    })
  } else {
    const subDirectoryName = path.substring(0, positionSlash + 1)
    const newPath = path.substring(positionSlash + 1)
    let subDirectory = tree.children.find(child => {
      return child.type === 'directory' && child.name === subDirectoryName
    })
    if (subDirectory === undefined) {
      subDirectory = {
        type: 'directory',
        name: subDirectoryName,
        children: []
      }
      tree.children.push(subDirectory)
    }
    importPath(newPath, fileContents, subDirectory)
  }
}
