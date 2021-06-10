
import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router);


import PaymentManager from "./components/PaymentManager"

import RoomManager from "./components/RoomManager"


import MyPage from "./components/MyPage"
import BookingManager from "./components/BookingManager"

import NotificationManager from "./components/NotificationManager"

export default new Router({
    // mode: 'history',
    base: process.env.BASE_URL,
    routes: [
            {
                path: '/Payment',
                name: 'PaymentManager',
                component: PaymentManager
            },

            {
                path: '/Room',
                name: 'RoomManager',
                component: RoomManager
            },


            {
                path: '/MyPage',
                name: 'MyPage',
                component: MyPage
            },
            {
                path: '/Booking',
                name: 'BookingManager',
                component: BookingManager
            },

            {
                path: '/Notification',
                name: 'NotificationManager',
                component: NotificationManager
            },



    ]
})
