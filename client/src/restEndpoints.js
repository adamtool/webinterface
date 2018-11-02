import * as axios from 'axios'

export default {existsWinningStrategy}
const existsWinningStrategy = (urlFormatter) => function (gameUUID) {
  axios.post(urlFormatter('/existsWinningStrategy'), {
    petriGameId: gameUUID
  }).then(response => {
    this.withErrorHandling(response, response => {
      this.petriGame.hasWinningStrategy = response.data.result
      // TODO consider displaying the info in a more persistent way, e.g. by colorizing the button "exists winning strategy".
      if (response.data.result) {
        this.showSuccessNotification('Yes, there is a winning strategy for this Petri Game.')
        // We expect an updated petriGame here because there might have been partition annotations added.
        this.petriGame.net = response.data.petriGame
      } else {
        this.showErrorNotification('No, there is no winning strategy for this Petri Game.')
      }
    })
  }).catch(() => {
    this.logError('Network error in existsWinningStrategy')
  })
}
