export {formatJobType}

function formatJobType (jobType) {
  switch (jobType) {
    case 'GRAPH_GAME_BDD':
      return 'Graph Game BDD'
    case 'EXISTS_WINNING_STRATEGY':
      return 'Exists winning strategy?'
    case 'WINNING_STRATEGY':
      return 'Winning Strategy'
    case 'GRAPH_STRATEGY_BDD':
      return 'Graph Strategy BDD'
    case 'MODEL_CHECKING_RESULT':
      return 'Model Checking Result'
    case 'MODEL_CHECKING_NET':
      return 'Model Checking Net'
    default:
      return jobType
  }
}