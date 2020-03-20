// Import all the preset APT examples for users to try out.
// See https://webpack.js.org/guides/dependency-management/#require-with-expression
const aptFilesContextSynthesis = require.context('./assets/apt-examples-synthesis', true, /\.apt$/)
const aptFilesContextModelChecking = require.context('./assets/apt-examples-mc', true, /\.apt$/)
const aptFileTreeSynthesis = {
  type: 'directory',
  name: 'root',
  children: []
}

const aptFileTreeModelChecking = {
  type: 'directory',
  name: 'root',
  children: []
}

importAll(aptFilesContextSynthesis, aptFileTreeSynthesis)
importAll(aptFilesContextModelChecking, aptFileTreeModelChecking)
export { aptFileTreeSynthesis, aptFileTreeModelChecking }

function importAll (r, tree) {
  r.keys().forEach(key => {
    // Use substring to discard the './' at the start of each path
    importPath(key.substring(2), r(key), tree)
  })
}

function importPath (path, fileContents, tree) {
  const positionSlash = path.indexOf('/')
  if (positionSlash === -1) {
    tree.children.push({
      type: 'file',
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
