function saveFileAs (text, filename) {
  // This function is adapted from https://stackoverflow.com/a/21016088
  const data = new Blob([text], {type: 'text/plain'})
  const textFileUrl = window.URL.createObjectURL(data)
  const link = document.createElement('a')
  link.setAttribute('download', filename)
  link.href = textFileUrl
  document.body.appendChild(link)
  // wait for the link to be added to the document
  window.requestAnimationFrame(function () {
    const event = new MouseEvent('click')
    link.dispatchEvent(event)
    document.body.removeChild(link)
    // Prevent memory leak by revoking the URL after it has been downloaded
    window.URL.revokeObjectURL(textFileUrl)
  })
}

export {
  saveFileAs
}
