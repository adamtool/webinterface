import {StyleFactory} from '@hscmap/vue-menu'

const style = StyleFactory((() => {
  const base = {
    backgroundColor: 'rgba(255, 255, 255, 0.9)',
    color: 'black',
    boxShadow: '0 2pt 6pt rgba(0, 0, 0, 0.5)'
  }
  return {
    menu: base,
    menubar: base,
    separator: { backgroundColor: 'rgba(127, 127, 127, 0.5)' },
    active: { backgroundColor: 'rgba(127, 127, 127, 0.75)', color: '#fff' },
    disabled: { opacity: '0.5' },
    animation: false
  }
})())

export default style
