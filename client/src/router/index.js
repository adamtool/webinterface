import Vue from 'vue'
import Router from 'vue-router'
import AppModelChecking from '../AppModelChecking'
import AppDistributedSynthesis from '../AppDistributedSynthesis'
import HomePage from '../HomePage'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path: '/modelChecking',
      name: 'AdamWEB - Model Checker',
      component: AppModelChecking
    },
    {
      path: '/',
      name: 'Home Page',
      component: HomePage
    },
    {
      path: '/distributedSynthesis',
      name: 'AdamWEB - Synthesizer',
      component: AppDistributedSynthesis
    }
  ]
})
