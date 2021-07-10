import {saveFileAs} from "./fileutilities";
import logging from "./logging";


function saveEditorNetAsPnml(restEndpoints, editorNetId, nodePositions, filename) {
  const requestPromise = restEndpoints.saveEditorNetAsPnml({
    editorNetId,
    nodeXYCoordinateAnnotations: nodePositions
  })
  requestPromise.then(response => {
    if (response.data.status === 'success') {
      const resultString = response.data.result
      saveFileAs(resultString, filename)
    } else if (response.data.status === 'error') {
      logging.sendErrorNotification(response.data.message)
    } else {
      logging.sendErrorNotification('Invalid response from server')
    }
  }).catch(logging.sendErrorNotification)
  return requestPromise
}

function saveEditorNetAsTikz(restEndpoints, editorNetId, nodePositions, filename) {
  const requestPromise = restEndpoints.saveEditorNetAsTikz({
    editorNetId,
    nodeXYCoordinateAnnotations: nodePositions
  })
  requestPromise.then(response => {
    if (response.data.status === 'success') {
      const resultString = response.data.result
      saveFileAs(resultString, filename)
    } else if (response.data.status === 'error') {
      logging.sendErrorNotification(response.data.message)
    } else {
      logging.sendErrorNotification('Invalid response from server')
    }
  }).catch(logging.sendErrorNotification)
  return requestPromise
}

function saveJobAsPnml(restEndpoints, jobKey, nodePositions, filename) {
  const requestPromise = restEndpoints.saveJobAsPnml({
    jobKey,
    nodeXYCoordinateAnnotations: nodePositions
  })
  requestPromise.then(response => {
    if (response.data.status === 'success') {
      const resultString = response.data.result
      saveFileAs(resultString, filename)
    } else if (response.data.status === 'error') {
      logging.sendErrorNotification(response.data.message)
    } else {
      logging.sendErrorNotification('Invalid response from server')
    }
  }).catch(logging.sendErrorNotification)
  return requestPromise
}

function saveJobAsApt(restEndpoints, jobKey, nodePositions, filename) {
  const requestPromise = restEndpoints.saveJobAsApt({
    jobKey,
    nodeXYCoordinateAnnotations: nodePositions
  })
  requestPromise.then(response => {
    if (response.data.status === 'success') {
      const apt = response.data.result
      saveFileAs(apt, filename)
    } else if (response.data.status === 'error') {
      logging.sendErrorNotification(response.data.message)
    } else {
      logging.sendErrorNotification('Invalid response from server')
    }
  }).catch(logging.sendErrorNotification)
  return requestPromise
}


export {saveJobAsPnml, saveEditorNetAsPnml, saveEditorNetAsTikz, saveJobAsApt}
