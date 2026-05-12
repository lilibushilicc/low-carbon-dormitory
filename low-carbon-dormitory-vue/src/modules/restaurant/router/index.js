import { createRouter, createWebHistory } from 'vue-router'
import index from '../views/index.vue'
import student from '../views/student.vue'
import studentMealWaste from '../views/MealWasteDaliy.vue' 
import week from '../views/week.vue'
import MealWasteWeek from '../views/MealWasteWeek.vue'
import MealWasteMonth from '../views/MealWasteMonth.vue'
import MealWasteTerm from '../views/MealWasteTerm.vue'
import MealWasteYear from '../views/MealWasteYear.vue'  
import month from '../views/month.vue'
import summary from '../views/summary.vue'
import mealAdd from '../views/mealAdd.vue'  
const routes = [
  {
    path: '/index',
    name: 'index',
    component: index
  },
  {
    path: '/index',
    component: index,
    redirect: '/index/summary',
    children: [
      {
        path: 'student/list',
        name: 'student',
        component: student
      },
      {
        path: 'student/add',
        name: 'mealAdd',
        component: mealAdd
      },
      {
        path: 'summary',
        name: 'summary',
        component: summary
      },
      {
        path: 'student/meal',
        component: () => import('../views/period.vue'),
        redirect: '/index/student/meal/daliy',
        children:[
          {
            path: 'daliy',
            name: 'studentMealWaste',
            component: studentMealWaste
          },{
            path: 'week',
            name: 'MealWasteWeek',
            component: MealWasteWeek
          },
          {
            path: 'month',
            name: 'MealWasteMonth',
            component: MealWasteMonth
          },
          {
            path: 'term',
            name: 'MealWasteTerm',
            component: MealWasteTerm
          },
          {
            path: 'year',
            name: 'MealWasteYear',
            component: MealWasteYear
          }

        ]
      },
      {
        path: 'student/period',
        component: () => import('../views/period.vue'),
        redirect: '/index/student/period/week',
        children: [
          {
            path: 'week',
            name: 'week',
            component: week
          },
          {
            path: 'month',
            name: 'month',
            component: month
          }
        ]
      }
    ]
  }
]
const router = createRouter({
  history: createWebHistory(),

  routes
})
export default router