
import Vue from 'vue'
import Router from 'vue-router'

Vue.use(Router);


import PaymentManager from "./components/PaymentManager"

import RoomManager from "./components/RoomManager"


import GuestManagementPage from "./components/GuestManagementPage"
import BookManager from "./components/BookManager"

import NotificationManager from "./components/NotificationManager"

export default new Router({
    // mode: 'history',
    base: process.env.BASE_URL,
    // base: "http://ac9d3799901094257960c3613da58e56-1202669080.ap-northeast-1.elb.amazonaws.com:8080",
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
                path: '/GuestManagementPage',
                name: 'GuestManagementPage',
                component: GuestManagementPage
            },
            {
                path: '/Book',
                name: 'BookManager',
                component: BookManager
            },

            {
                path: '/Notification',
                name: 'NotificationManager',
                component: NotificationManager
            },



    ]
})
