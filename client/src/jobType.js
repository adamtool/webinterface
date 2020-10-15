export {formatJobType, modelCheckingResultColor, formatSatisfied}

function formatJobType(jobType) {
  switch (jobType) {
    case 'GRAPH_GAME_BDD':
      return '2-Player Game'
    case 'EXISTS_WINNING_STRATEGY':
      return 'Exists winning strategy?'
    case 'WINNING_STRATEGY':
      return 'Winning Strategy'
    case 'GRAPH_STRATEGY_BDD':
      return '2-Player Strategy'
    case 'MODEL_CHECKING_RESULT':
      return 'Model Checking Result'
    case 'MODEL_CHECKING_NET':
      return 'Model Checking Net'
    case 'MODEL_CHECKING_FORMULA':
      return 'Model Checking Formula'
    default:
      return jobType
  }
}

function modelCheckingResultColor(satisfied) {
  switch (satisfied) {
    case 'TRUE':
      return '#00bbbb'
    case 'FALSE':
    case 'UNKNOWN':
      return 'red'
    default:
      logging.logError('Missing switch case in JobList.modelCheckingResultColor' +
        ' for the case "' + satisfied + '".')
      return 'red'
  }
}

function formatSatisfied(satisfied) {
  console.log('satisfied: ' + satisfied)
  if (satisfied === 'TRUE') {
    return 'Satisfied'
  } else if (satisfied === 'FALSE') {
    return 'Not satisfied'
  } else if (satisfied === 'UNKNOWN') {
    return 'Unknown'
  } else {
    return satisfied
  }
}
