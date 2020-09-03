export {formatJobType, modelCheckingResultColor}

function formatJobType (jobType) {
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
    default:
      return jobType
  }
}

function modelCheckingResultColor (satisfied) {
  switch (satisfied) {
    case 'TRUE':
      return 'blue'
    case 'FALSE':
    case 'UNKNOWN':
      return 'red'
    default:
      logging.logError('Missing switch case in JobList.modelCheckingResultColor' +
        ' for the case "' + satisfied + '".')
      return 'red'
  }
}
