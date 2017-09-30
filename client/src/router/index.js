import Vue from 'vue'
import Router from 'vue-router'
import Hello from '@/components/Hello'
import AnnTest from '@/components/AnnTest'
import AptEditor from '@/components/AptEditor'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path: '/hello',
      name: 'Hello',
      component: Hello
    },
    {
      path: '/test',
      name: 'Ann test',
      component: AnnTest
    },
    {
      path: '/',
      name: 'Editor',
      component: AptEditor
    }
  ]
})
