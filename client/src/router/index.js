import Vue from 'vue'
import Router from 'vue-router'
import AppModelChecking from '../AppModelChecking'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path: '/modelChecking',
      name: 'ADAM Web for Model Checking',
      component: AppModelChecking
    },
    {
      path: '/',
      name: 'Adam Web',
      component: AppModelChecking
    }
  ]
})
