// Stuff to help aid development of the UI

// E.g. if you are working on the appearance of tabs, which have transient states like 'running'
// that in practice quickly disappear, it is helpful to be able to freeze the state of the
// application and have a set of tabs that will not suddenly change state and will not
// require you to repeatedly trigger new jobs in order to see how the new tabs look.

// The state of the art on this is called 'time traveling debugging', which is based on using a
// central data store (Flux, VueX) to hold all of the state of your application using immutable data
// structures so that you can jump back and forth whenever you want to any intermediate state of
// the data store.

// Unfortunately, our project is not set up quite like that.  There are mutable data
// structures in use, and state is distributed among multiple components, so this is the
// best we can do right now.
export {
  fakeTabs
}

const fakeTabs = [
  {
    'type': 'MODEL_CHECKING_RESULT',
    'jobKey': {},
    'jobStatus': 'RUNNING',
    'timeStarted': 1571050631,
    'timeFinished': 0,
    'queuePosition': 0,
    'name': 'MODEL_CHECKING_RESULT',
    'uuid': '{"canonicalApt":".name \\"Simple Example\\"\\n.description \\"A very small APT example to reduce CPU load during development and show all the different ways that nodes in a graph can look\\"\\n.type LPN\\n.options\\ncondition=\\"LTL\\"\\n\\n.places\\nE1[itfl=\\"true\\", yCoord=632.88, bad=\\"true\\", xCoord=349.64]\\nE2[itfl=\\"true\\", yCoord=641.01, xCoord=705.01]\\np0[yCoord=347.47, xCoord=513.28, bad=\\"true\\"]\\np1[yCoord=399.06, xCoord=706.54]\\np2[itfl=\\"true\\", yCoord=396.93, xCoord=359.28, bad=\\"true\\", env=\\"true\\"]\\np3[itfl=\\"true\\", yCoord=713.19, xCoord=483.51, env=\\"true\\"]\\np4[bad=\\"true\\", env=\\"true\\"]\\np5[env=\\"true\\"]\\n\\n.transitions\\nt1[label=\\"t1\\", yCoord=517.96, xCoord=535.35, tfl=\\"p0 -> {p1},p2 -> {p3},E1 -> {E2}\\"]\\n\\n.flows\\nt1: {1*E1, 1*p0, 1*p2, 1*p4} -> {1*p5, 1*p1, 1*p3, 1*E2}\\n\\n.initial_marking {1*E1, 99*E2, 1*p2, 1*p3}","requestParams":{"formula":"A p4"},"jobType":"MODEL_CHECKING_RESULT"}',
    'isCloseable': true
  },
  {
    'type': 'MODEL_CHECKING_RESULT',
    'jobKey': {},
    'jobStatus': 'FAILED',
    'timeStarted': 1571050830,
    'timeFinished': 1571050830,
    'failureReason': 'java.lang.RuntimeException: The atom \'p44\' is no identifier of a place or a transition of the net \'Simple Example\'.\nThe places are [Node{id=E1}, Node{id=E2}, Node{id=p0}, Node{id=p1}, Node{id=p2}, Node{id=p3}, Node{id=p4}, Node{id=p5}]\nThe transitions are [Node{id=t1}]',
    'queuePosition': -1,
    'name': 'MODEL_CHECKING_RESULT',
    'uuid': '{"canonicalApt":".name \\"Simple Example\\"\\n.description \\"A very small APT example to reduce CPU load during development and show all the different ways that nodes in a graph can look\\"\\n.type LPN\\n.options\\ncondition=\\"LTL\\"\\n\\n.places\\nE1[itfl=\\"true\\", yCoord=632.88, bad=\\"true\\", xCoord=349.64]\\nE2[itfl=\\"true\\", yCoord=641.01, xCoord=705.01]\\np0[yCoord=347.47, xCoord=513.28, bad=\\"true\\"]\\np1[yCoord=399.06, xCoord=706.54]\\np2[itfl=\\"true\\", yCoord=396.93, xCoord=359.28, bad=\\"true\\", env=\\"true\\"]\\np3[itfl=\\"true\\", yCoord=713.19, xCoord=483.51, env=\\"true\\"]\\np4[bad=\\"true\\", env=\\"true\\"]\\np5[env=\\"true\\"]\\n\\n.transitions\\nt1[label=\\"t1\\", yCoord=517.96, xCoord=535.35, tfl=\\"p0 -> {p1},p2 -> {p3},E1 -> {E2}\\"]\\n\\n.flows\\nt1: {1*E1, 1*p0, 1*p2, 1*p4} -> {1*p5, 1*p1, 1*p3, 1*E2}\\n\\n.initial_marking {1*E1, 99*E2, 1*p2, 1*p3}","requestParams":{"formula":"A p44"},"jobType":"MODEL_CHECKING_RESULT"}',
    'isCloseable': true
  }
]
