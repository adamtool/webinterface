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

const aptFileListSynthesis = []
const aptFileListModelChecking = []

importAll(aptFilesContextSynthesis, aptFileTreeSynthesis, aptFileListSynthesis)
importAll(aptFilesContextModelChecking, aptFileTreeModelChecking, aptFileListModelChecking)
export { aptFileTreeSynthesis, aptFileTreeModelChecking, aptFileListSynthesis, aptFileListModelChecking }

function importAll (r, tree, fileList) {
  r.keys().forEach(key => {
    // Use substring to discard the './' at the start of each path
    importPath(key.substring(2), r(key), tree, fileList)
  })
}

function importPath (path, fileContents, tree, fileList) {
  const positionSlash = path.indexOf('/')
  if (positionSlash === -1) {
    tree.children.push({
      type: 'file',
      name: path,
      body: fileContents
    })
    fileList.push({
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
    importPath(newPath, fileContents, subDirectory, fileList)
  }
}
