import Vue from 'vue'
import Router from 'vue-router'
import Hello from '@/components/Hello'
import AnnTest from '@/components/AnnTest'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path: '/',
      name: 'Hello',
      component: Hello
    },
    {
      path: '/test',
      name: 'Ann test',
      component: AnnTest
    }
  ]
})
