import Vue from "vue";
import App from "./App.vue";
import vuetify from "./plugins/vuetify";
import Managing from "./components";
import router from './router'
Vue.config.productionTip = false;

const axios = require("axios").default;

// backend host url
axios.backend = "http://a60ac02ae722f4bbf8b8a84c1ce84d8f-105579122.ap-northeast-2.elb.amazonaws.com:8080";

axios.backendUrl = new URL(axios.backend);
axios.fixUrl = function(original){

  var url = null;

  try{
    url = new URL(original);
  }catch(e){
    url = new URL(axios.backend + original);
  }

  url.hostname = axios.backendUrl.hostname;
  url.port = axios.backendUrl.port;

  return url.href;
}

const templateFiles = require.context("./components", true);
Vue.prototype.$ManagerLists = [];
templateFiles.keys().forEach(function(tempFiles) {
  if (!tempFiles.includes("Manager.vue") && tempFiles.includes("vue")) {
    Vue.prototype.$ManagerLists.push(
      tempFiles.replace("./", "").replace(".vue", "")
    );
  }
});

Vue.use(Managing);

new Vue({
  vuetify,
  router,
  render: h => h(App)
}).$mount("#app");
