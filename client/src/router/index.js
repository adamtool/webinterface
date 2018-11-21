import Vue from 'vue'
import Router from 'vue-router'
import AppModelChecking from '../AppModelChecking'
import AppOtherApproach from '../AppOtherApproach'
import HomePage from '../HomePage'

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
      name: 'Home Page',
      component: HomePage
    },
    {
      path: '/otherApproach',
      name: 'ADAM Web (not for model checking)', // TODO rename
      component: AppOtherApproach
    }
  ]
})
